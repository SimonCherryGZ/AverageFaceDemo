#include <stdio.h>
#include <jni.h>
#include <stdlib.h>
//#include <iostream>
//#include <fstream>
//#include <dirent.h>
#include <opencv2/opencv.hpp>
#include<android/log.h>
/* Header for class com_simoncherry_jnidemo_JNIUtils */
#include "jni_app.h"
#include "json/json.h"

#define LOG    "AverageFace-jni" // 这个是自定义的LOG的标识
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG,__VA_ARGS__) // 定义LOGF类型

#ifdef __cplusplus
extern "C" {
#endif

using namespace cv;
using namespace std;
IplImage * change4channelTo3InIplImage(IplImage * src);

IplImage * change4channelTo3InIplImage(IplImage * src) {
    if (src->nChannels != 4) {
        return NULL;
    }

    IplImage * destImg = cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 3);
    for (int row = 0; row < src->height; row++) {
        for (int col = 0; col < src->width; col++) {
            CvScalar s = cvGet2D(src, row, col);
            cvSet2D(destImg, row, col, s);
        }
    }

    return destImg;
}

/*
 * Class:     com_simoncherry_averageface_JNIUtils
 * Method:    doGrayScale
 * Signature: ([III)[I
 */
JNIEXPORT jintArray JNICALL Java_com_simoncherry_averageface_JNIUtils_doGrayScale
        (JNIEnv *env, jclass obj, jintArray buf, jint w, jint h) {
    LOGE("doGrayScale Start");

    jint *cbuf;
    cbuf = env->GetIntArrayElements(buf, JNI_FALSE);
    if (cbuf == NULL) {
        return 0;
    }

    cv::Mat imgData(h, w, CV_8UC4, (unsigned char *) cbuf);

    uchar* ptr = imgData.ptr(0);
    for(int i = 0; i < w*h; i ++){
        //计算公式：Y(亮度) = 0.299*R + 0.587*G + 0.114*B
        //对于一个int四字节，其彩色值存储方式为：BGRA
        int grayScale = (int)(ptr[4*i+2]*0.299 + ptr[4*i+1]*0.587 + ptr[4*i+0]*0.114);
        ptr[4*i+1] = grayScale;
        ptr[4*i+2] = grayScale;
        ptr[4*i+0] = grayScale;
    }

    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, cbuf);
    env->ReleaseIntArrayElements(buf, cbuf, 0);

    LOGE("doGrayScale End");
    return result;
}

/*
 * Class:     com_simoncherry_averageface_JNIUtils
 * Method:    doEdgeDetection
 * Signature: ([III)[I
 */
JNIEXPORT jintArray JNICALL Java_com_simoncherry_averageface_JNIUtils_doEdgeDetection
        (JNIEnv *env, jclass obj, jintArray buf, jint w, jint h) {
    jint *cbuf;
    cbuf = env->GetIntArrayElements(buf, JNI_FALSE);
    if (cbuf == NULL) {
        return 0;
    }

    cv::Mat myimg(h, w, CV_8UC4, (unsigned char*) cbuf);
    IplImage image = IplImage(myimg);
    IplImage* image3channel = change4channelTo3InIplImage(&image);

    IplImage* pCannyImage = cvCreateImage(cvGetSize(image3channel),IPL_DEPTH_8U,1);
    cvCanny(image3channel, pCannyImage, 50, 150, 3);

    int* outImage = new int[w*h];
    for(int i=0;i<w*h;i++) {
        outImage[i]=(int)pCannyImage->imageData[i];
    }

    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, outImage);
    env->ReleaseIntArrayElements(buf, cbuf, 0);
    return result;
}

/*
 * Class:     com_simoncherry_averageface_JNIUtils
 * Method:    doBinaryzation
 * Signature: ([III)[I
 */
JNIEXPORT jintArray JNICALL Java_com_simoncherry_averageface_JNIUtils_doBinaryzation
        (JNIEnv *env, jclass obj, jintArray buf, jint w, jint h) {
    jint *cbuf;
    cbuf = env->GetIntArrayElements(buf, JNI_FALSE);
    if (cbuf == NULL) {
        return 0;
    }

    cv::Mat imgData(h, w, CV_8UC4, (unsigned char *) cbuf);
    IplImage image = IplImage(imgData);
    IplImage *pSrcImage = change4channelTo3InIplImage(&image);
    IplImage *g_pGrayImage = cvCreateImage(cvGetSize(pSrcImage), IPL_DEPTH_8U, 1);
    cvCvtColor(pSrcImage, g_pGrayImage, CV_BGR2GRAY);

    IplImage *g_pBinaryImage = cvCreateImage(cvGetSize(g_pGrayImage), IPL_DEPTH_8U, 1);
    cvThreshold(g_pGrayImage, g_pBinaryImage, 127, 255, CV_THRESH_BINARY);

    int* outImage = new int[w * h];
    for(int i=0;i<w*h;i++) {
        outImage[i]=(int)g_pBinaryImage->imageData[i];
    }

    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, outImage);
    env->ReleaseIntArrayElements(buf, cbuf, 0);
    return result;
}

jstring str2jstring(JNIEnv* env, const char* pat) {
    //定义java String类 strClass
    jclass strClass = (env)->FindClass("Ljava/lang/String;");
    //获取String(byte[],String)的构造器,用于将本地byte[]数组转换为一个新String
    jmethodID ctorID = (env)->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    //建立byte数组
    jbyteArray bytes = (env)->NewByteArray(strlen(pat));
    //将char* 转换为byte数组
    (env)->SetByteArrayRegion(bytes, 0, strlen(pat), (jbyte*)pat);
    // 设置String, 保存语言类型,用于byte数组转换至String时的参数
    jstring encoding = (env)->NewStringUTF("GB2312");
    //将byte数组转换为java String,并输出
    return (jstring)(env)->NewObject(strClass, ctorID, bytes, encoding);
}

std::string jstring2str(JNIEnv* env, jstring jstr) {
    char* rtn = NULL;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("GB2312");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr= (jbyteArray)env->CallObjectMethod(jstr,mid,strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte* ba = env->GetByteArrayElements(barr,JNI_FALSE);
    if(alen > 0) {
        rtn = (char*)malloc(alen+1);
        memcpy(rtn,ba,alen);
        rtn[alen]=0;
    }
    env->ReleaseByteArrayElements(barr,ba,0);
    std::string stemp(rtn);
    free(rtn);
    return stemp;
}

const char* string2printf(JNIEnv *env, std::string str) {
    jstring jstr = env->NewStringUTF(str.c_str());
    return env->GetStringUTFChars(jstr, NULL);
}

JNIEXPORT void JNICALL Java_com_simoncherry_averageface_JNIUtils_testParseJson
    (JNIEnv *env, jclass obj, jstring str) {
    LOGE("testParseJson Start");

    const char* json_str = env->GetStringUTFChars(str, NULL);
    LOGE("printf %s", json_str);

//    // to build a json object with id and name
//    Json::Value user;
//    user["id"] = 12345;
//    user["name"] = "Tony";
//    const char* json_str = user.toStyledString().c_str();
//    LOGE("printf %s", json_str);

//    std::string strValue = "{\"key1\":\"value1\",\"array\":[ {\"key2\":\"value2\"},{\"key2\":\"value3\"},{\"key2\":\"value4\"}]}";
//    Json::Reader reader;
//    Json::Value value;
//    if (reader.parse(strValue, value)) {
//        std::string out = value["key1"].asString();
//        LOGE("printf %s", string2printf(env, out));
//
//        const Json::Value arrayObj = value["array"];
//        for (int i = 0; i < arrayObj.size(); i++) {
//            out = arrayObj[i]["key2"].asString();
//            LOGE("printf %s", string2printf(env, out));
//        }
//    }

    std::string strValue = jstring2str(env, str);
    Json::Reader reader;
    Json::Value value;
    if (reader.parse(strValue, value)) {
        const Json::Value arrayObj = value["landmark"];
        for (int i=0; i<arrayObj.size(); i++) {
            std::string x = arrayObj[i]["x"].asString();
            std::string y = arrayObj[i]["y"].asString();
            std::string output = std::string("landmark ") + std::string("x=") + x + std::string(", y=") + y;
            jstring jstr = env->NewStringUTF(output.c_str());
            const char* json_str = env->GetStringUTFChars(jstr, NULL);
            LOGE("printf %s", json_str);
        }
    }

    LOGE("testParseJson End");
}

//// Compute similarity transform given two pairs of corresponding points.
//// OpenCV requires 3 pairs of corresponding points.
//// We are faking the third one.
//void similarityTransform(std::vector<cv::Point2f>& inPoints, std::vector<cv::Point2f>& outPoints, cv::Mat &tform) {
//    double s60 = sin(60 * M_PI / 180.0);
//    double c60 = cos(60 * M_PI / 180.0);
//
//    vector <Point2f> inPts = inPoints;
//    vector <Point2f> outPts = outPoints;
//
//    inPts.push_back(cv::Point2f(0,0));
//    outPts.push_back(cv::Point2f(0,0));
//
//
//    inPts[2].x =  c60 * (inPts[0].x - inPts[1].x) - s60 * (inPts[0].y - inPts[1].y) + inPts[1].x;
//    inPts[2].y =  s60 * (inPts[0].x - inPts[1].x) + c60 * (inPts[0].y - inPts[1].y) + inPts[1].y;
//
//    outPts[2].x =  c60 * (outPts[0].x - outPts[1].x) - s60 * (outPts[0].y - outPts[1].y) + outPts[1].x;
//    outPts[2].y =  s60 * (outPts[0].x - outPts[1].x) + c60 * (outPts[0].y - outPts[1].y) + outPts[1].y;
//
//
//    tform = cv::estimateRigidTransform(inPts, outPts, false);
//}
//
//// Calculate Delaunay triangles for set of points
//// Returns the vector of indices of 3 points for each triangle
//static void calculateDelaunayTriangles(Rect rect, vector<Point2f> &points, vector< vector<int> > &delaunayTri){
//
//    // Create an instance of Subdiv2D
//    Subdiv2D subdiv(rect);
//
//    // Insert points into subdiv
//    for( vector<Point2f>::iterator it = points.begin(); it != points.end(); it++)
//        subdiv.insert(*it);
//
//    vector<Vec6f> triangleList;
//    subdiv.getTriangleList(triangleList);
//    vector<Point2f> pt(3);
//    vector<int> ind(3);
//
//    for( size_t i = 0; i < triangleList.size(); i++ )
//    {
//        Vec6f t = triangleList[i];
//        pt[0] = Point2f(t[0], t[1]);
//        pt[1] = Point2f(t[2], t[3]);
//        pt[2] = Point2f(t[4], t[5 ]);
//
//        if ( rect.contains(pt[0]) && rect.contains(pt[1]) && rect.contains(pt[2])){
//            for(int j = 0; j < 3; j++)
//                for(size_t k = 0; k < points.size(); k++)
//                    if(abs(pt[j].x - points[k].x) < 1.0 && abs(pt[j].y - points[k].y) < 1)
//                        ind[j] = k;
//
//            delaunayTri.push_back(ind);
//        }
//    }
//}
//
//// Apply affine transform calculated using srcTri and dstTri to src
//void applyAffineTransform(Mat &warpImage, Mat &src, vector<Point2f> &srcTri, vector<Point2f> &dstTri) {
//    // Given a pair of triangles, find the affine transform.
//    Mat warpMat = getAffineTransform( srcTri, dstTri );
//
//    // Apply the Affine Transform just found to the src image
//    warpAffine( src, warpImage, warpMat, warpImage.size(), INTER_LINEAR, BORDER_REFLECT_101);
//}
//
//
//// Warps and alpha blends triangular regions from img1 and img2 to img
//void warpTriangle(Mat &img1, Mat &img2, vector<Point2f> t1, vector<Point2f> t2) {
//    // Find bounding rectangle for each triangle
//    Rect r1 = boundingRect(t1);
//    Rect r2 = boundingRect(t2);
//
//    // Offset points by left top corner of the respective rectangles
//    vector<Point2f> t1Rect, t2Rect;
//    vector<Point> t2RectInt;
//    for(int i = 0; i < 3; i++)
//    {
//        //tRect.push_back( Point2f( t[i].x - r.x, t[i].y -  r.y) );
//        t2RectInt.push_back( Point((int)(t2[i].x - r2.x), (int)(t2[i].y - r2.y)) ); // for fillConvexPoly
//
//        t1Rect.push_back( Point2f( t1[i].x - r1.x, t1[i].y -  r1.y) );
//        t2Rect.push_back( Point2f( t2[i].x - r2.x, t2[i].y - r2.y) );
//    }
//
//    // Get mask by filling triangle
//    Mat mask = Mat::zeros(r2.height, r2.width, CV_32FC3);
//    fillConvexPoly(mask, t2RectInt, Scalar(1.0, 1.0, 1.0), 16, 0);
//
//    // Apply warpImage to small rectangular patches
//    Mat img1Rect, img2Rect;
//    img1(r1).copyTo(img1Rect);
//
//    Mat warpImage = Mat::zeros(r2.height, r2.width, img1Rect.type());
//
//    applyAffineTransform(warpImage, img1Rect, t1Rect, t2Rect);
//
//    // Copy triangular region of the rectangular patch to the output image
//    multiply(warpImage,mask, warpImage);
//    multiply(img2(r2), Scalar(1.0,1.0,1.0) - mask, img2(r2));
//    img2(r2) = img2(r2) + warpImage;
//}
//
//// Constrains points to be inside boundary
//void constrainPoint(Point2f &p, Size sz) {
//    p.x = min(max( (double)p.x, 0.0), (double)(sz.width - 1));
//    p.y = min(max( (double)p.y, 0.0), (double)(sz.height - 1));
//}
//
//// Read points from list of text file
//void readPoints(vector<string> pointsFileNames, vector<vector<Point2f> > &pointsVec) {
//
//    for(size_t i = 0; i < pointsFileNames.size(); i++) {
//        vector<Point2f> points;
//        ifstream ifs(pointsFileNames[i]);
//        float x, y;
//        while(ifs >> x >> y)
//            points.push_back(Point2f((float)x, (float)y));
//
//        pointsVec.push_back(points);
//    }
//}
//
//// Read names from the directory
//void readFileNames(string dirName, vector<string> &imageFnames, vector<string> &ptsFnames) {
//    DIR *dir;
//    struct dirent *ent;
//    int count = 0;
//
//    //image extensions
//    string imgExt = "jpg";
//    string txtExt = "txt";
//
//    if ((dir = opendir (dirName.c_str())) != NULL) {
//        /* print all the files and directories within directory */
//        while ((ent = readdir (dir)) != NULL) {
//            if(count < 2) {
//                count++;
//                continue;
//            }
//
//            string path = dirName;
//            string fname = ent->d_name;
//
//            if (fname.find(imgExt, (fname.length() - imgExt.length())) != std::string::npos) {
//                path.append(fname);
//                imageFnames.push_back(path);
//            }
//            else if (fname.find(txtExt, (fname.length() - txtExt.length())) != std::string::npos) {
//                path.append(fname);
//                ptsFnames.push_back(path);
//            }
//
//        }
//        closedir (dir);
//    }
//}

#ifdef __cplusplus
}
#endif
