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

ImageFormat* getImageFormat(JNIEnv* env, jobject jimft) {
    if (jimft == NULL) {
        throwNPException(env, "Error: NativeImageFormat object is NULL");
        return NULL;
    }

    // 防御性校验：检查传入的 jobject 是否是有效的 JNI 引用
    jobjectRefType refType = (*env)->GetObjectRefType(env, jimft);
    if (refType == JNIInvalidRefType) {
        throwNPException(env, "Error: NativeImageFormat object reference is invalid");
        return NULL;
    }

    // 安全获取当前实例对应的 Class
    jclass clazz = (*env)->GetObjectClass(env, jimft);
    if (clazz == NULL) {
        throwNPException(env, "Error: GetObjectClass returned NULL");
        return NULL;
    }

    // 动态获取属性 ID
    jfieldID cmmFormatID = (*env)->GetFieldID(env, clazz, "cmmFormat", "I");
    jfieldID colsID = (*env)->GetFieldID(env, clazz, "cols", "I");
    jfieldID rowsID = (*env)->GetFieldID(env, clazz, "rows", "I");
    jfieldID scanlineStrideID = (*env)->GetFieldID(env, clazz, "scanlineStride", "I");
    jfieldID imageDataID = (*env)->GetFieldID(env, clazz, "imageData", "Ljava/lang/Object;");
    jfieldID dataOffsetID = (*env)->GetFieldID(env, clazz, "dataOffset", "I");
    jfieldID alphaOffsetID = (*env)->GetFieldID(env, clazz, "alphaOffset", "I");

    // 如果属性 ID 获取失败（比如类被 R8/Proguard 混淆），清空异常并安全返回
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        (*env)->DeleteLocalRef(env, clazz);
        throwNPException(env, "Error: Failed to obtain field IDs for NativeImageFormat. Is it obfuscated?");
        return NULL;
    }

    // 分配内存
    ImageFormat *imft = malloc(sizeof(ImageFormat));
    if (imft == NULL) {
        (*env)->DeleteLocalRef(env, clazz);
        throwNewOutOfMemoryError(env, "Error: out of memory allocating ImageFormat");
        return NULL;
    }

    // 提取 Java 属性
    imft->cmmFormat = (int) (*env)->GetIntField(env, jimft, cmmFormatID);
    imft->cols = (int) (*env)->GetIntField(env, jimft, colsID);
    imft->rows = (int) (*env)->GetIntField(env, jimft, rowsID);
    imft->scanlineStride = (int) (*env)->GetIntField(env, jimft, scanlineStrideID);
    imft->dataOffset = (int) (*env)->GetIntField(env, jimft, dataOffsetID);
    imft->alphaOffset = (int) (*env)->GetIntField(env, jimft, alphaOffsetID);

    imft->jImageData = (jarray) (*env)->GetObjectField(env, jimft, imageDataID);
    
    // 及时释放 clazz 局部引用避免 Local Reference Table 溢出
    (*env)->DeleteLocalRef(env, clazz);

    if (imft->jImageData == NULL) {
        free(imft);
        throwNPException(env, "Error: NativeImageFormat.imageData array is NULL");
        return NULL;
    }

    imft->imageData = (BYTE*) (*env)->GetPrimitiveArrayCritical(env, imft->jImageData, 0);
    if(imft->imageData == NULL) { 
        throwNPException(env, "Error while accessing java image data");
        releaseImageFormat(env, imft);
        return NULL;
    }

    return imft;
}

void releaseImageFormat(JNIEnv* env, ImageFormat* imft) {
    if(imft == NULL) return; 

    if(imft->imageData != NULL && imft->jImageData != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, imft->jImageData, imft->imageData, 0);
    }

    free(imft);
}