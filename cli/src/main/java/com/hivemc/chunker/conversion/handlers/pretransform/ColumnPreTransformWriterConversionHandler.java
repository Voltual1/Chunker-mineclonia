// [file name]: com.hivemc.chunker.conversion.handlers.pretransform.ColumnPreTransformConversionHandler.java
package com.hivemc.chunker.conversion.handlers.pretransform;

import com.google.common.base.Preconditions;
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ColumnPreTransformConversionHandler implements ColumnConversionHandler {
    private final ColumnConversionHandler delegate;
    private final Map<RegionCoordPair, Map<ChunkCoordPair, ColumnData>> pending = new Object2ReferenceOpenHashMap<>();
    private final Set<RegionCoordPair> incompleteRegions = new ObjectOpenHashSet<>();
    private final Set<ChunkCoordPair> processedColumns = new ObjectOpenHashSet<>();
    private final List<ColumnData> cachedPendingSolving = new ArrayList<>();
    private final Stack<ColumnData> cachedSolvingStack = new Stack<>();
    private final Set<ColumnData> cachedChecking = new ObjectOpenHashSet<>();
    private final Set<ColumnData> cachedSolved = new ObjectOpenHashSet<>();

    public ColumnPreTransformConversionHandler(ColumnConversionHandler delegate, ChunkerWorld chunkerWorld) {
        this.delegate = delegate;
        incompleteRegions.addAll(chunkerWorld.getRegions());
    }

    protected void trySolve(ColumnData input, Set<ColumnData> output) {
        cachedSolvingStack.push(input);
        while (!cachedSolvingStack.isEmpty()) {
            ColumnData current = cachedSolvingStack.pop();
            if (!current.getPendingCheckEdges().isEmpty()) {
                cachedChecking.clear();
                break;
            } else {
                cachedChecking.add(current);
                for (ColumnData value : current.getRequiredColumns().values()) {
                    if (value == null) continue;
                    if (cachedChecking.add(value)) {
                        cachedSolvingStack.push(value);
                    }
                }
            }
        }
        if (!cachedChecking.isEmpty()) {
            output.addAll(cachedChecking);
            cachedChecking.clear();
        }
        cachedSolvingStack.clear();
    }

    protected void processPendingSolve() {
        if (!cachedPendingSolving.isEmpty()) {
            for (ColumnData pendingSolve : cachedPendingSolving) {
                trySolve(pendingSolve, cachedSolved);
            }
            cachedPendingSolving.clear();

            if (!cachedSolved.isEmpty()) {
                transformCluster(cachedSolved);
                for (ColumnData value : cachedSolved) {
                    RegionCoordPair regionCoordPair = value.getPosition().getRegion();
                    Map<ChunkCoordPair, ColumnData> region = pending.get(regionCoordPair);
                    region.remove(value.getPosition());
                }
                cachedSolved.clear();
            }
        }
    }

    protected boolean solveEdge(ChunkCoordPair position, Edge edge, @Nullable ColumnData columnData) {
        Edge opposite = edge.getOpposite();
        ChunkCoordPair relativePosition = edge.getRelative(position);
        RegionCoordPair relativePositionRegion = relativePosition.getRegion();
        Map<ChunkCoordPair, ColumnData> region = pending.get(relativePosition.getRegion());
        ColumnData relativeData = null;

        if (region != null) relativeData = region.get(relativePosition);

        if (relativeData == null) {
            return !incompleteRegions.contains(relativePositionRegion);
        } else {
            if (columnData != null && columnData.getRequiredColumns().containsKey(edge)) {
                relativeData.getRequiredColumns().put(opposite, columnData);
                columnData.getRequiredColumns().put(edge, relativeData);
            }
            if (relativeData.getPendingCheckEdges().remove(opposite)) {
                if (columnData != null && relativeData.getRequiredColumns().containsKey(opposite)) {
                    relativeData.getRequiredColumns().put(opposite, columnData);
                    columnData.getRequiredColumns().put(edge, relativeData);
                }
                if (relativeData.getRequiredColumns().isEmpty() && relativeData.getPendingCheckEdges().isEmpty()) {
                    region.remove(relativePosition);
                    relativeData.submit(delegate);
                } else if (relativeData.getPendingCheckEdges().isEmpty()) {
                    cachedPendingSolving.add(relativeData);
                }
            }
        }
        return true;
    }

    protected void solveColumn(ColumnData columnData) {
        ChunkCoordPair position = columnData.getPosition();
        synchronized (this) {
            Preconditions.checkArgument(processedColumns.add(position), "Duplicate chunk processed, unable to solve.");
            columnData.getPendingCheckEdges().removeIf(edge -> solveEdge(position, edge, columnData));
            if (columnData.getRequiredColumns().isEmpty() && columnData.getPendingCheckEdges().isEmpty()) {
                columnData.submit(delegate);
            } else {
                Map<ChunkCoordPair, ColumnData> region = pending.computeIfAbsent(position.getRegion(), (ignored) -> new Object2ReferenceOpenHashMap<>());
                region.put(position, columnData);
                if (columnData.getPendingCheckEdges().isEmpty()) {
                    cachedPendingSolving.add(columnData);
                }
            }
            processPendingSolve();
        }
    }

    @Override
    public Task<Void> convertColumn(ChunkerColumn column) {
        Set<Edge> outgoingEdges = column.getRequiredPreTransformEdges();
        EnumSet<Edge> pendingCheckEdges = EnumSet.allOf(Edge.class);
        Map<Edge, ColumnData> requiredColumns = new EnumMap<>(Edge.class);

        if (outgoingEdges != null) {
            for (Edge edge : outgoingEdges) requiredColumns.put(edge, null);
        }

        ColumnData columnData = new ColumnData(column.getPosition(), column, requiredColumns, pendingCheckEdges);
        solveColumn(columnData);
        return Task.async("PreTransform Convert", TaskWeight.LOW, () -> {});
    }

    @Override
    public Task<Void> flushRegion(RegionCoordPair regionCoordPair) {
        synchronized (this) {
            incompleteRegions.remove(regionCoordPair);
            Map<ChunkCoordPair, ColumnData> region = pending.get(regionCoordPair);

            if (region == null) {
                return delegate.flushRegion(regionCoordPair);
            }

            transformCluster(region.values());
            region.clear();
            pending.remove(regionCoordPair);

            markAsEmpty(regionCoordPair.getChunk(0, 0), EnumSet.of(Edge.NEGATIVE_X, Edge.NEGATIVE_Z));
            markAsEmpty(regionCoordPair.getChunk(31, 0), EnumSet.of(Edge.POSITIVE_X, Edge.NEGATIVE_Z));
            markAsEmpty(regionCoordPair.getChunk(0, 31), EnumSet.of(Edge.NEGATIVE_X, Edge.POSITIVE_Z));
            markAsEmpty(regionCoordPair.getChunk(31, 31), EnumSet.of(Edge.POSITIVE_X, Edge.POSITIVE_Z));

            for (int x = 1; x < 31; x++) {
                markAsEmpty(regionCoordPair.getChunk(x, 0), EnumSet.of(Edge.NEGATIVE_Z));
                markAsEmpty(regionCoordPair.getChunk(x, 31), EnumSet.of(Edge.POSITIVE_Z));
            }

            for (int z = 1; z < 31; z++) {
                markAsEmpty(regionCoordPair.getChunk(0, z), EnumSet.of(Edge.NEGATIVE_X));
                markAsEmpty(regionCoordPair.getChunk(31, z), EnumSet.of(Edge.POSITIVE_X));
            }
        }
        return delegate.flushRegion(regionCoordPair);
    }

    protected void markAsEmpty(ChunkCoordPair chunkCoordPair, EnumSet<Edge> checkedEdges) {
        if (processedColumns.contains(chunkCoordPair)) return;
        checkedEdges.forEach(edge -> solveEdge(chunkCoordPair, edge, null));
        processPendingSolve();
    }

    protected void handlePreTransform(ChunkerColumn column, Map<Edge, ColumnData> requiredColumns) {
        Map<Edge, ChunkerColumn> columns = new Object2ObjectOpenHashMap<>(requiredColumns.size());
        requiredColumns.forEach(((edge, columnData) -> {
            if (columnData == null) return;
            columns.put(edge, columnData.getColumn());
        }));
        column.preTransform(columns);
    }

    protected void transformCluster(Collection<ColumnData> cluster) {
        for (ColumnData pendingData : cluster) handlePreTransform(pendingData.getColumn(), pendingData.getRequiredColumns());
        for (ColumnData pendingData : cluster) pendingData.submit(delegate);
    }

    @Override
    public Task<Void> flushColumns() {
        return Task.async("Submitting remaining columns", TaskWeight.NORMAL, () -> {
            synchronized (this) {
                for (Map.Entry<RegionCoordPair, Map<ChunkCoordPair, ColumnData>> region : pending.entrySet()) {
                    transformCluster(region.getValue().values());
                }
                pending.clear();
            }
        }).thenUnwrap("Calling delegate flushColumns", TaskWeight.NORMAL, () -> delegate.flushColumns());
    }

    protected static class ColumnData {
        private final ChunkCoordPair position;
        private final ChunkerColumn column;
        private final Map<Edge, ColumnData> requiredColumns;
        private final EnumSet<Edge> pendingCheckEdges;
        private boolean submitted;

        public ColumnData(ChunkCoordPair position, ChunkerColumn column, Map<Edge, ColumnData> requiredColumns, EnumSet<Edge> pendingCheckEdges) {
            this.position = position;
            this.column = column;
            this.requiredColumns = requiredColumns;
            this.pendingCheckEdges = pendingCheckEdges;
        }
        public ChunkCoordPair getPosition() { return position; }
        public ChunkerColumn getColumn() { return column; }
        public Map<Edge, ColumnData> getRequiredColumns() { return requiredColumns; }
        public EnumSet<Edge> getPendingCheckEdges() { return pendingCheckEdges; }

        public void submit(ColumnConversionHandler delegate) {
            Preconditions.checkArgument(!submitted, "Duplicate submission occurred for column!");
            submitted = true;
            delegate.convertColumn(column);
        }
    }
}