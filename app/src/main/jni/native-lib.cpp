#include <jni.h>
#include "image_utils.h"

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_flameassignmentrd_app_NativeBridge_processNV21(
        JNIEnv *env,
        jobject /* thiz */,
        jbyteArray nv21_,
        jint width,
        jint height
) {
    jbyte* nv21 = env->GetByteArrayElements(nv21_, nullptr);

    cv::Mat frame = nv21ToMat(nv21, width, height);
    cv::Mat processed = applyCanny(frame);

    int size = processed.total() * processed.elemSize();
    jbyteArray out = env->NewByteArray(size);
    env->SetByteArrayRegion(out, 0, size, reinterpret_cast<jbyte*>(processed.data));

    env->ReleaseByteArrayElements(nv21_, nv21, 0);
    return out;
}