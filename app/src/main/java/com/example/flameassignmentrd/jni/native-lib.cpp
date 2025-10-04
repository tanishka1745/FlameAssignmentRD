#include <jni.h>
#include <opencv2/opencv.hpp>
#include "opencv_processor.cpp"

using namespace cv;

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_flameassignmentrd_NativeBridge_processNV21(JNIEnv *env, jobject thiz,
                                                            jbyteArray nv21_,
                                                            jint width, jint height) {
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