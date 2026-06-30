/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Oleg V. Khaschansky
 * 
 */

#include "NativeImageFormat.h"
#include "exceptions.h"
#include <pthread.h>

jfieldID clr_NIF_cmmFormatID = NULL;
jfieldID clr_NIF_colsID = NULL;
jfieldID clr_NIF_rowsID = NULL;
jfieldID clr_NIF_scanlineStrideID = NULL;
jfieldID clr_NIF_imageDataID = NULL;
jfieldID clr_NIF_dataOffsetID = NULL;
jfieldID clr_NIF_alphaOffsetID = NULL;

static pthread_mutex_t nif_init_mutex = PTHREAD_MUTEX_INITIALIZER;
static volatile int nif_ids_initialized = 0;

static jfieldID safe_get_field_id(JNIEnv* env, jclass clazz, const char* name, const char* sig) {
    jfieldID fid = (*env)->GetFieldID(env, clazz, name, sig);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        return NULL;
    }
    return fid;
}

ImageFormat* getImageFormat(JNIEnv* env, jobject jimft) {
    if (jimft == NULL) {
        throwNPException(env, "Error: NativeImageFormat object is NULL");
        return NULL;
    }

    if (!nif_ids_initialized) {
        pthread_mutex_lock(&nif_init_mutex);
        if (!nif_ids_initialized) {
            jclass clazz = (*env)->GetObjectClass(env, jimft);
            if (clazz != NULL) {
                clr_NIF_cmmFormatID = safe_get_field_id(env, clazz, "cmmFormat", "I");
                clr_NIF_colsID = safe_get_field_id(env, clazz, "cols", "I");
                clr_NIF_rowsID = safe_get_field_id(env, clazz, "rows", "I");
                clr_NIF_scanlineStrideID = safe_get_field_id(env, clazz, "scanlineStride", "I");
                clr_NIF_imageDataID = safe_get_field_id(env, clazz, "imageData", "Ljava/lang/Object;");
                clr_NIF_dataOffsetID = safe_get_field_id(env, clazz, "dataOffset", "I");
                clr_NIF_alphaOffsetID = safe_get_field_id(env, clazz, "alphaOffset", "I");
                (*env)->DeleteLocalRef(env, clazz);
                nif_ids_initialized = 1;
            }
        }
        pthread_mutex_unlock(&nif_init_mutex);
    }

    // 详尽的安全检查，防止 Proguard/R8 混淆或者字段被裁剪导致 NULL 崩溃
    if (clr_NIF_cmmFormatID == NULL) {
        throwNPException(env, "JNI Error: NativeImageFormat.cmmFormat field ID is NULL. Make sure Proguard rules keep this field.");
        return NULL;
    }
    if (clr_NIF_colsID == NULL) {
        throwNPException(env, "JNI Error: NativeImageFormat.cols field ID is NULL. Make sure Proguard rules keep this field.");
        return NULL;
    }
    if (clr_NIF_rowsID == NULL) {
        throwNPException(env, "JNI Error: NativeImageFormat.rows field ID is NULL. Make sure Proguard rules keep this field.");
        return NULL;
    }
    if (clr_NIF_scanlineStrideID == NULL) {
        throwNPException(env, "JNI Error: NativeImageFormat.scanlineStride field ID is NULL. Make sure Proguard rules keep this field.");
        return NULL;
    }
    if (clr_NIF_imageDataID == NULL) {
        throwNPException(env, "JNI Error: NativeImageFormat.imageData field ID is NULL. Make sure Proguard rules keep this field.");
        return NULL;
    }
    if (clr_NIF_dataOffsetID == NULL) {
        throwNPException(env, "JNI Error: NativeImageFormat.dataOffset field ID is NULL. Make sure Proguard rules keep this field.");
        return NULL;
    }
    if (clr_NIF_alphaOffsetID == NULL) {
        throwNPException(env, "JNI Error: NativeImageFormat.alphaOffset field ID is NULL. Make sure Proguard rules keep this field.");
        return NULL;
    }

    // Create the structure.
    ImageFormat *imft = malloc(sizeof(ImageFormat));
    if (imft == NULL) {
        throwNewOutOfMemoryError(env, "Error: out of memory allocating ImageFormat");
        return NULL;
    }

    imft->cmmFormat = (int) (*env)->GetIntField(env, jimft, clr_NIF_cmmFormatID);
    imft->cols = (int) (*env)->GetIntField(env, jimft, clr_NIF_colsID);
    imft->rows = (int) (*env)->GetIntField(env, jimft, clr_NIF_rowsID);
    imft->scanlineStride = (int) (*env)->GetIntField(env, jimft, clr_NIF_scanlineStrideID);
    imft->dataOffset = (int) (*env)->GetIntField(env, jimft, clr_NIF_dataOffsetID);
    imft->alphaOffset = (int) (*env)->GetIntField(env, jimft, clr_NIF_alphaOffsetID);

    // Get image data
    imft->jImageData = (jarray) (*env)->GetObjectField(env, jimft, clr_NIF_imageDataID);
    if (imft->jImageData == NULL) {
        free(imft);
        throwNPException(env, "Error: NativeImageFormat.imageData array is NULL");
        return NULL;
    }

    imft->imageData = (BYTE*) (*env)->GetPrimitiveArrayCritical(env, imft->jImageData, 0);
    if(imft->imageData == NULL) { // All is lost, we don't have C array
        throwNPException(env, "Error while accessing java image data");
        // Free resources and stop further processing...
        releaseImageFormat(env, imft);
        return NULL;
    }

    return imft;
}

void releaseImageFormat(JNIEnv* env, ImageFormat* imft) {
    if(imft == NULL) return; // nothing to do

    // Release java array
    if(imft->imageData != NULL && imft->jImageData != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, imft->jImageData, imft->imageData, 0);
    }

    // Free memory
    free(imft);
}