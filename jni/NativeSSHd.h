#include <jni.h>

#ifndef _Included_NativeSSHd
#define _Included_NativeSSHd

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     NativeSSHd
 * Method:    runCommand
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_tk_tanguy_droidsshd_system_NativeSSHd_runCommand
  (JNIEnv *, jclass, jstring);

/*
 * Class:     NativeSSHd
 * Method:    chmod
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_tk_tanguy_droidsshd_system_NativeSSHd_chmod
  (JNIEnv *, jclass, jstring, jint);

/*
 * Class:     NativeSSHd
 * Method:    symlink
 * Signature: (Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_tk_tanguy_droidsshd_system_NativeSSHd_symlink
  (JNIEnv *, jclass, jstring, jstring);

#ifdef __cplusplus
}
#endif

#endif
