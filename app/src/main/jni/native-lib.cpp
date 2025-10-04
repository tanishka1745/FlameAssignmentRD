#include <jni.h>
#include <opencv2/opencv.hpp>
using namespace cv;


// Converts NV21 camera byte array to an OpenCV BGR Mat
Mat nv21ToMat(jbyte* nv21, int width, int height) {
    Mat yuv(height + height / 2, width, CV_8UC1, (unsigned char*)nv21);
    Mat bgr;
    cvtColor(yuv, bgr, COLOR_YUV2BGR_NV21);
    return bgr;
}
// Applies Canny edge detection on an input BGR image
Mat applyCanny(Mat &input) {
    Mat gray, edges;
    cvtColor(input, gray, COLOR_BGR2GRAY);
    // Apply Canny edge detection
    // 100 = lower threshold, 200 = upper threshold
    Canny(gray, edges, 100, 200);
    cvtColor(edges, edges, COLOR_GRAY2BGR);
    return edges;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_flameassignmentrd_app_NativeBridge_processNV21(
        JNIEnv *env,
        jobject thiz,
        jbyteArray nv21_,
        jint width,
        jint height
) {
    // Convert input jbyteArray to native byte pointer
    jbyte* nv21 = env->GetByteArrayElements(nv21_, nullptr);

    // Convert NV21 to OpenCV Mat
    Mat frame = nv21ToMat(nv21, width, height);

    // Apply Canny Edge Detection
    Mat processed = applyCanny(frame);

    // Convert Mat back to byte array
    int size = processed.total() * processed.elemSize();
    jbyteArray out = env->NewByteArray(size);
    env->SetByteArrayRegion(out, 0, size, (jbyte*)processed.data);

    // Release input array
    env->ReleaseByteArrayElements(nv21_, nv21, 0);
    return out;
}