#include <stdio.h>
#include <jni.h>
#include <stdlib.h>
//#include <opencv2/opencv.hpp>
/* Header for class com_simoncherry_jnidemo_JNIUtils */
#include "jni_app.h"

//using namespace cv;
//IplImage * change4channelTo3InIplImage(IplImage * src);
//
//IplImage * change4channelTo3InIplImage(IplImage * src) {
//    if (src->nChannels != 4) {
//        return NULL;
//    }
//
//    IplImage * destImg = cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 3);
//    for (int row = 0; row < src->height; row++) {
//        for (int col = 0; col < src->width; col++) {
//            CvScalar s = cvGet2D(src, row, col);
//            cvSet2D(destImg, row, col, s);
//        }
//    }
//
//    return destImg;
//}
//
///*
// * Class:     com_simoncherry_averageface_JNIUtils
// * Method:    doGrayScale
// * Signature: ([III)[I
// */
//JNIEXPORT jintArray JNICALL Java_com_simoncherry_averageface_JNIUtils_doGrayScale
//        (JNIEnv *env, jclass obj, jintArray buf, jint w, jint h) {
//    jint *cbuf;
//    cbuf = env->GetIntArrayElements(buf, JNI_FALSE);
//    if (cbuf == NULL) {
//        return 0;
//    }
//
//    cv::Mat imgData(h, w, CV_8UC4, (unsigned char *) cbuf);
//
//    uchar* ptr = imgData.ptr(0);
//    for(int i = 0; i < w*h; i ++){
//        //计算公式：Y(亮度) = 0.299*R + 0.587*G + 0.114*B
//        //对于一个int四字节，其彩色值存储方式为：BGRA
//        int grayScale = (int)(ptr[4*i+2]*0.299 + ptr[4*i+1]*0.587 + ptr[4*i+0]*0.114);
//        ptr[4*i+1] = grayScale;
//        ptr[4*i+2] = grayScale;
//        ptr[4*i+0] = grayScale;
//    }
//
//    int size = w * h;
//    jintArray result = env->NewIntArray(size);
//    env->SetIntArrayRegion(result, 0, size, cbuf);
//    env->ReleaseIntArrayElements(buf, cbuf, 0);
//    return result;
//}
//
///*
// * Class:     com_simoncherry_averageface_JNIUtils
// * Method:    doEdgeDetection
// * Signature: ([III)[I
// */
//JNIEXPORT jintArray JNICALL Java_com_simoncherry_averageface_JNIUtils_doEdgeDetection
//        (JNIEnv *env, jclass obj, jintArray buf, jint w, jint h) {
//    jint *cbuf;
//    cbuf = env->GetIntArrayElements(buf, JNI_FALSE);
//    if (cbuf == NULL) {
//        return 0;
//    }
//
//    cv::Mat myimg(h, w, CV_8UC4, (unsigned char*) cbuf);
//    IplImage image = IplImage(myimg);
//    IplImage* image3channel = change4channelTo3InIplImage(&image);
//
//    IplImage* pCannyImage = cvCreateImage(cvGetSize(image3channel),IPL_DEPTH_8U,1);
//    cvCanny(image3channel, pCannyImage, 50, 150, 3);
//
//    int* outImage = new int[w*h];
//    for(int i=0;i<w*h;i++) {
//        outImage[i]=(int)pCannyImage->imageData[i];
//    }
//
//    int size = w * h;
//    jintArray result = env->NewIntArray(size);
//    env->SetIntArrayRegion(result, 0, size, outImage);
//    env->ReleaseIntArrayElements(buf, cbuf, 0);
//    return result;
//}
//
///*
// * Class:     com_simoncherry_averageface_JNIUtils
// * Method:    doBinaryzation
// * Signature: ([III)[I
// */
//JNIEXPORT jintArray JNICALL Java_com_simoncherry_averageface_JNIUtils_doBinaryzation
//        (JNIEnv *env, jclass obj, jintArray buf, jint w, jint h) {
//    jint *cbuf;
//    cbuf = env->GetIntArrayElements(buf, JNI_FALSE);
//    if (cbuf == NULL) {
//        return 0;
//    }
//
//    cv::Mat imgData(h, w, CV_8UC4, (unsigned char *) cbuf);
//    IplImage image = IplImage(imgData);
//    IplImage *pSrcImage = change4channelTo3InIplImage(&image);
//    IplImage *g_pGrayImage = cvCreateImage(cvGetSize(pSrcImage), IPL_DEPTH_8U, 1);
//    cvCvtColor(pSrcImage, g_pGrayImage, CV_BGR2GRAY);
//
//    IplImage *g_pBinaryImage = cvCreateImage(cvGetSize(g_pGrayImage), IPL_DEPTH_8U, 1);
//    cvThreshold(g_pGrayImage, g_pBinaryImage, 127, 255, CV_THRESH_BINARY);
//
//    int* outImage = new int[w * h];
//    for(int i=0;i<w*h;i++) {
//        outImage[i]=(int)g_pBinaryImage->imageData[i];
//    }
//
//    int size = w * h;
//    jintArray result = env->NewIntArray(size);
//    env->SetIntArrayRegion(result, 0, size, outImage);
//    env->ReleaseIntArrayElements(buf, cbuf, 0);
//    return result;
//}
