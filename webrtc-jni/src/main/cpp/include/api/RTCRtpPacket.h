#ifndef JNI_RTC_RTP_PACKET_H_
#define JNI_RTC_RTP_PACKET_H_

#include "JavaRef.h"
#include "api/rtp_packet.h"
#include <jni.h>

namespace jni {
namespace RTCRtpPacket {

    JavaLocalRef<jobject> toJava(JNIEnv* env, const webrtc::RtpPacket& packet);
    
    // Allows updating a native packet from a Java packet
    void toNative(JNIEnv* env, jobject jPacket, webrtc::RtpPacket& packet);

} // namespace RTCRtpPacket
} // namespace jni

#endif // JNI_RTC_RTP_PACKET_H_
