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

#include "NativeCMM.h"
#include <string.h>

static LCMSBOOL cmsInitialized = FALSE; 
static char *errMsg = NULL;

int gl_cmsErrorHandler(int errorCode, const char *msg) {
  if(errorCode == LCMS_ERRC_ABORTED) {
    // Throw exception later, after returning control from cmm
#if defined(ZOS) || defined(LINUX) || defined(FREEBSD) || defined(MACOSX)
    errMsg = strdup(msg);
#else
    errMsg = strdup(msg);
#endif
  }

  return 1;
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmOpenProfile
 * Signature: ([B)J
 */
/*JNIEXPORT*/ jlong /*JNICALL*/
    Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmOpenProfile(JNIEnv *env, jclass cls, jbyteArray data)
{
  jbyte *byteData = (*env)->GetByteArrayElements (env, data, 0);
  DWORD dataSize = (*env)->GetArrayLength (env, data);
    cmsHPROFILE hProfile;

  // Set up error handling if needed
  if(!cmsInitialized) {
    cmsErrorAction(LCMS_ERROR_SHOW);
    cmsSetErrorHandler(gl_cmsErrorHandler);
    cmsInitialized = TRUE;
  }

    hProfile = cmmOpenProfile((LPBYTE)byteData, dataSize);

    (*env)->ReleaseByteArrayElements (env, data, byteData, 0);

    if(hProfile == NULL) {
        newCMMException(env, errMsg); // Throw java exception if error occured
        free(errMsg);
        errMsg = NULL;
    }

  // Return obtained handle
  return (jlong) (hProfile);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmCloseProfile
 * Signature: (J)V
 */
/*JNIEXPORT*/ void /*JNICALL*/
    Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmCloseProfile(JNIEnv *env, jclass cls, jlong profileID)
{
  cmsHPROFILE hProfile = (cmsHPROFILE) (profileID);

    if(!cmsCloseProfile(hProfile)) {
        newCMMException(env, errMsg); // Throw java exception if error occured
        free(errMsg);
        errMsg = NULL;
    }        
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmGetProfileSize
 * Signature: (J)I
 */
/*JNIEXPORT*/ jint /*JNICALL*/
    Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmGetProfileSize(JNIEnv *env, jclass cls, jlong profileID)
{
  cmsHPROFILE hProfile = (cmsHPROFILE) (profileID);
  return (jint) cmmGetProfileSize(hProfile);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmGetProfile
 * Signature: (J[B)V
 */
/*JNIEXPORT*/ void /*JNICALL*/ Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmGetProfile(
    JNIEnv *env,
    jclass cls,
    jlong profileID,
    jbyteArray data)
{
    cmsHPROFILE hProfile = (cmsHPROFILE) (profileID);
    unsigned profileSize = (unsigned) (*env)->GetArrayLength (env, data);
    jbyte *byteData = (*env)->GetByteArrayElements(env, data, 0);

  cmmGetProfile(hProfile, (LPBYTE)byteData, profileSize);

    (*env)->ReleaseByteArrayElements (env, data, byteData, 0);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmGetProfileElement
 * Signature: (JI[B)V
 */
/*JNIEXPORT*/ void /*JNICALL*/ Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmGetProfileElement
  (JNIEnv *env, jclass cls, jlong profileID, jint tagSignature, jbyteArray data)
{
  size_t dataSize = (*env)->GetArrayLength(env, data);
    icTagSignature ts = tagSignature;
    cmsHPROFILE hProfile = (cmsHPROFILE) (profileID);
    jbyte *byteData = (*env)->GetByteArrayElements (env, data, 0);



  if(ts == HEADER_TAG_ID) {
        if(!cmmGetProfileHeader(hProfile, (LPBYTE)byteData, dataSize)) {
            newCMMException(env, errMsg); // Throw java exception if error occured
            free(errMsg);
            errMsg = NULL;
        }
    } else {
        if(!cmmGetProfileElement(hProfile, ts, (LPBYTE)byteData, &dataSize)) {
            newCMMException(env, errMsg); // Throw java exception if error occured
            free(errMsg);
            errMsg = NULL;
        }
    }

    (*env)->ReleaseByteArrayElements (env, data, byteData, 0);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmGetProfileElementSize
 * Signature: (JI)I
 */
/*JNIEXPORT*/ jint /*JNICALL*/ Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmGetProfileElementSize
  (JNIEnv *env, jclass cls, jlong profileID, jint tagSignature)
{

    long size;
    icTagSignature ts = tagSignature;
    cmsHPROFILE hProfile = (cmsHPROFILE) (profileID);

    if (ts == HEADER_TAG_ID) {
        size = HEADER_SIZE;
    } else {
        size = cmmGetProfileElementSize(hProfile, ts);
    }

  if(size < 0)
    newCMMException(env, "Profile element not found"); // Throw java exception if error occured

    return (jint) size;
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmSetProfileElement
 * Signature: (JI[B)V
 */
/*JNIEXPORT*/ void /*JNICALL*/ Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmSetProfileElement
  (JNIEnv *env, jclass cls, jlong profileID, jint tagSignature, jbyteArray data)
{
    cmsHPROFILE hProfile = (cmsHPROFILE) (profileID);
    jbyte *byteData = (*env)->GetByteArrayElements (env, data, 0);
    size_t dataSize = (*env)->GetArrayLength(env, data);
    icTagSignature ts = tagSignature;

    if(ts == HEADER_TAG_ID) {
    if(dataSize != sizeof(icHeader))
      newCMMException(env, "Invalid size of the data"); // Throw java exception 

        if(!cmmSetProfileHeader(hProfile, (LPBYTE)byteData))
            newCMMException(env, "Invalid header data"); // Throw java exception if error occured

    } else {
      if(!cmmSetProfileElement(hProfile, ts, byteData, dataSize)) {
            newCMMException(env, errMsg); // Throw java exception if error occured
            free(errMsg);
            errMsg = NULL;
      }
    }

    (*env)->ReleaseByteArrayElements (env, data, byteData, 0);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmCreateMultiprofileTransform
 * Signature: ([J[I)J
 */
/*JNIEXPORT*/ jlong /*JNICALL*/ Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmCreateMultiprofileTransform
(JNIEnv *env, jclass cls, jlongArray jProfileHandles, jintArray jRenderingIntents) {
  cmsHTRANSFORM xform;

  jint *renderingIntentsData;
  int intent, i;

  int nProfiles = (*env)->GetArrayLength (env, jProfileHandles);
    jlong *profileHandlesData = (*env)->GetLongArrayElements (env, jProfileHandles, 0);

    // Convert to appropriate size
    cmsHPROFILE *profileHandles = malloc(sizeof(cmsHPROFILE)*nProfiles);
    for(i=0; i<nProfiles; i++) {
      profileHandles[i] = (cmsHPROFILE) (profileHandlesData[i]);
    }

  // XXX - Todo - consider getting all rendering intents
    renderingIntentsData = (*env)->GetIntArrayElements(env, jRenderingIntents, 0);
  intent = renderingIntentsData[0];

  xform = cmmCreateMultiprofileTransform(profileHandles, nProfiles, intent);

    (*env)->ReleaseLongArrayElements(env, jProfileHandles, profileHandlesData, 0);
    (*env)->ReleaseIntArrayElements(env, jRenderingIntents, renderingIntentsData, 0);
  
  free(profileHandles);

  if(xform == NULL) 
    newCMMException(env, "Can't create ICC transform"); // Throw java exception

  return (jlong) (xform);
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmDeleteTransform
 * Signature: (J)V
 */
/*JNIEXPORT*/ void /*JNICALL*/ Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmDeleteTransform
  (JNIEnv *env, jclass cls, jlong transformHandle)
{
    cmsHTRANSFORM xform = (cmsHTRANSFORM) (transformHandle);

    if(xform != NULL) {
        cmsDeleteTransform(xform);
    } else
        throwNPException(env, "Invalid ICC transform passed to CMM");
}

static long getScanlineStrideFromFormat(ImageFormat *fmt) {
  return 
    fmt->cols * 
    T_BYTES(fmt->cmmFormat) * 
    (T_CHANNELS(fmt->cmmFormat) + T_EXTRA(fmt->cmmFormat));
}

static long getPixelStrideFromFormat(ImageFormat *fmt) {
  return 
    T_BYTES(fmt->cmmFormat) * 
    (T_CHANNELS(fmt->cmmFormat) + T_EXTRA(fmt->cmmFormat));
}

static long getSampleSizeFromFormat(ImageFormat *fmt) {
  return T_BYTES(fmt->cmmFormat) ? T_BYTES(fmt->cmmFormat) : 8;
}

static void copyAlphaChannel(
  BYTE *srcPtr, BYTE *dstPtr, 
  int srcPixelStride, int dstPixelStride, 
  int srcSampleSize, int dstSampleSize,
  int nPixels
) {
  register LPBYTE src = srcPtr, dst = dstPtr;
  int i;

  if(srcSampleSize == 1 && dstSampleSize == 1) {
    for(i=0; i<nPixels; i++) {
      *dst=*src;
      src += srcPixelStride;
      dst += dstPixelStride;
    }
  } else if(srcSampleSize == 2 && dstSampleSize == 2) {
    for(i=0; i<nPixels; i++) {      
      *dst=*src;
      *(dst+1)=*(src+1);
      src += srcPixelStride;
      dst += dstPixelStride;
    }
  } else if(srcSampleSize == 2 && dstSampleSize == 1) {
    for(i=0; i<nPixels; i++) {
      *((LPWORD)dst)=RGB_8_TO_16(*src);
      src += srcPixelStride;
      dst += dstPixelStride;
    }
  } else if(srcSampleSize == 1 && dstSampleSize == 2) {
    for(i=0; i<nPixels; i++) {
      *dst=RGB_16_TO_8(*((LPWORD)src));
      src += srcPixelStride;
      dst += dstPixelStride;
    }
  } else { // All other sample types, very slow
    double d = 0;
    
    double srcMax = 1 << srcSampleSize*8;
    double dstMax = 1 << dstSampleSize*8;

    for(i=0; i<nPixels; i++) {
      if(srcSampleSize == 0) {
        d = *((double *) (src));
      } else {
        int sh;
        for(sh=0; sh<srcSampleSize; sh++) {
          d+=((*(src+sh)) << (sh*8));
        }
        d /= (srcMax-1);        
      }

      if(dstSampleSize == 0) {
        *((double *) (src)) = d;
      } else {
        int sh;
        long l = (long)(d * (dstMax-1) + 0.5);        
        for(sh=0; sh<dstSampleSize; sh++) {
          *(dst+sh) = (BYTE) (l % 256);
          l /= 256;
        }        
      }

      src += srcPixelStride;
      dst += dstPixelStride;
    }
  }
}

/*
 * Class:     org_apache_harmony_awt_gl_color_NativeCMM
 * Method:    cmmTranslateColors
 * Signature: (JLorg/apache/harmony/awt/gl/color/NativeImageFormat;Lorg/apache/harmony/awt/gl/color/NativeImageFormat;)V
 */ 
/*JNIEXPORT*/ void /*JNICALL*/ Java_org_apache_harmony_awt_gl_color_NativeCMM_cmmTranslateColors
  (JNIEnv *env, jclass cls, jlong transformHandle, jobject src, jobject dst)
{
  int srcPixels, dstPixels;
  ImageFormat *srcFormat = NULL, *dstFormat = NULL;
  int srcSampleSize, dstSampleSize;
  int srcPixelStride, dstPixelStride;
  BYTE *srcPtr, *dstPtr;
  int i;
  LCMSBOOL copyAlpha = FALSE;
  LCMSBOOL fillAlpha = FALSE;

  cmsHTRANSFORM xform = (cmsHTRANSFORM) (transformHandle);

  if (xform == NULL) {
      throwNPException(env, "Error: transformHandle is NULL");
      return;
  }

  srcFormat = getImageFormat(env, src);
  dstFormat = getImageFormat(env, dst);

  // 安全检查：如果任何一个图像格式读取失败，立即清理并退出，防止后续空指针访问
  if (srcFormat == NULL || dstFormat == NULL) {
      if (srcFormat != NULL) releaseImageFormat(env, srcFormat);
      if (dstFormat != NULL) releaseImageFormat(env, dstFormat);
      return;
  }

  // Do we have to copy alpha?
  copyAlpha = srcFormat->alphaOffset >= 0 && dstFormat->alphaOffset >= 0;
  fillAlpha = dstFormat->alphaOffset >= 0;

  if(copyAlpha) {
    srcSampleSize = getSampleSizeFromFormat(srcFormat);
    dstSampleSize = getSampleSizeFromFormat(dstFormat);
    srcPixelStride = getPixelStrideFromFormat(srcFormat);
    dstPixelStride = getPixelStrideFromFormat(dstFormat);
  } else if(fillAlpha) {
    dstPixelStride = getPixelStrideFromFormat(dstFormat);
  }

  srcPixels = srcFormat->cols * srcFormat->rows;
  dstPixels = dstFormat->cols * dstFormat->rows;
  
  cmsChangeBuffersFormat(xform, srcFormat->cmmFormat, dstFormat->cmmFormat);

  srcPtr = srcFormat->imageData + srcFormat->dataOffset;
  dstPtr = dstFormat->imageData + dstFormat->dataOffset;
  
  if(srcFormat->scanlineStride < 0 && dstFormat->scanlineStride < 0) {
    
    if(copyAlpha) { // Copy alpha
      copyAlphaChannel(
        srcPtr + srcFormat->alphaOffset, dstPtr + dstFormat->alphaOffset, 
        srcPixelStride, dstPixelStride, 
        srcSampleSize, dstSampleSize, 
        dstPixels
      ); 
    } else if(fillAlpha) { // Fill with 1's
      memset(dstPtr, 0xFF, dstPixels*dstPixelStride); 
    }

    cmsDoTransform(xform, srcPtr, dstPtr, dstPixels); // Call LCMS!

  } else { // Process each scanline
    if(srcFormat->scanlineStride < 0)
      srcFormat->scanlineStride = getScanlineStrideFromFormat(srcFormat);
    if(dstFormat->scanlineStride < 0)
      dstFormat->scanlineStride = getScanlineStrideFromFormat(dstFormat);

    for(i=0; i<srcFormat->rows; i++) {
      
      if(copyAlpha) { // Copy Alpha
        copyAlphaChannel(
          srcPtr + srcFormat->alphaOffset, dstPtr + dstFormat->alphaOffset, 
          srcPixelStride, dstPixelStride, 
          srcSampleSize, dstSampleSize, 
          srcFormat->cols
        ); 
      } else if(fillAlpha) { // Fill with 1's
        memset(dstPtr, 0xFF, dstFormat->cols*dstPixelStride); 
      }

      cmsDoTransform(xform, srcPtr, dstPtr, dstFormat->cols);

      srcPtr += srcFormat->scanlineStride;
      dstPtr += dstFormat->scanlineStride;
    }
  }

  releaseImageFormat(env, srcFormat);
  releaseImageFormat(env, dstFormat);
}