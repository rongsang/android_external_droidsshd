#include <jni.h>

#ifndef _Included_libNativeSSHd
#define _Included_libNativeSSHd
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     libNativeSSHd
 * Method:    runCommand
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_libNativeSSHd_runCommand
  (JNIEnv *, jclass, jstring);

/*
 * Class:     libNativeSSHd
 * Method:    chmod
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_libNativeSSHd_chmod
  (JNIEnv *, jclass, jstring, jint);

/*
 * Class:     libNativeSSHd
 * Method:    symlink
 * Signature: (Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_libNativeSSHd_symlink
  (JNIEnv *, jclass, jstring, jstring);

#ifdef __cplusplus
}
#endif
#endif
