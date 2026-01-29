#include "api/RTCRtpPacket.h"
#include "api/RTCRtpHeaderExtension.h"
#include "JavaClasses.h"
#include "JavaList.h"
#include "JavaObject.h"
#include "JavaUtils.h"
#include "JNI_WebRTC.h"

#include "api/rtp_packet.h"

namespace jni {

namespace RTCRtpPacket {

    class JavaRTCRtpHeaderClass : public JavaClass {
    public:
        explicit JavaRTCRtpHeaderClass(JNIEnv* env) {
            cls = FindClass(env, PKG "RTCRtpHeader");
            ctor = GetMethod(env, cls, "<init>", "()V");
            payloadType = GetFieldID(env, cls, "payloadType", "I");
            sequenceNumber = GetFieldID(env, cls, "sequenceNumber", "I");
            timestamp = GetFieldID(env, cls, "timestamp", "J");
            ssrc = GetFieldID(env, cls, "ssrc", "J");
            csrcs = GetFieldID(env, cls, "csrcs", LIST_SIG);
        }

        jclass cls;
        jmethodID ctor;
        jfieldID payloadType;
        jfieldID sequenceNumber;
        jfieldID timestamp;
        jfieldID ssrc;
        jfieldID csrcs;
    };

    class JavaRTCRtpPacketClass : public JavaClass {
    public:
        explicit JavaRTCRtpPacketClass(JNIEnv* env) {
            cls = FindClass(env, PKG "RTCRtpPacket");
            ctor = GetMethod(env, cls, "<init>", "(L" PKG "RTCRtpHeader;" BYTE_BUFFER_SIG LIST_SIG ")V");
            header = GetFieldID(env, cls, "header", "L" PKG "RTCRtpHeader;");
            payload = GetFieldID(env, cls, "payload", BYTE_BUFFER_SIG);
            extensions = GetFieldID(env, cls, "extensions", LIST_SIG);
        }

        jclass cls;
        jmethodID ctor;
        jfieldID header;
        jfieldID payload;
        jfieldID extensions;
    };

    JavaLocalRef<jobject> toJava(JNIEnv* env, const webrtc::RtpPacket& packet) {
        const auto headerClass = JavaClasses::get<JavaRTCRtpHeaderClass>(env);
        const auto packetClass = JavaClasses::get<JavaRTCRtpPacketClass>(env);

        // Create Header
        jobject jHeader = env->NewObject(headerClass->cls, headerClass->ctor);
        env->SetIntField(jHeader, headerClass->payloadType, packet.PayloadType());
        env->SetIntField(jHeader, headerClass->sequenceNumber, packet.SequenceNumber());
        env->SetLongField(jHeader, headerClass->timestamp, packet.Timestamp());
        env->SetLongField(jHeader, headerClass->ssrc, packet.Ssrc());
        
        std::vector<uint32_t> csrcs = packet.Csrcs();
        std::vector<long> csrcsLong;
        for (auto csrc : csrcs) csrcsLong.push_back(static_cast<long>(csrc));
        
        env->SetObjectField(jHeader, headerClass->csrcs, JavaList::toArrayList(env, csrcsLong, &JavaUtils::toJavaLong));

        // Create Payload ByteBuffer (Direct)
        auto payload = packet.payload();
        jobject jPayload = env->NewDirectByteBuffer(const_cast<uint8_t*>(payload.data()), payload.size());

        // Create Extensions
        // Note: webrtc::RtpPacket doesn't easily expose all extensions as a list of RtpExtension
        // For now we might leave it empty or implement specialized extraction if needed.
        JavaLocalRef<jobject> jExtensions = JavaList::createArrayList(env, 0);

        jobject jPacket = env->NewObject(packetClass->cls, packetClass->ctor, jHeader, jPayload, jExtensions.get());

        return JavaLocalRef<jobject>(env, jPacket);
    }

    void toNative(JNIEnv* env, jobject jPacket, webrtc::RtpPacket& packet) {
        // Implementation for Modifying packets if needed.
        // For Audio-only monitoring/simple modification, we might just need to update payload if changed.
        const auto packetClass = JavaClasses::get<JavaRTCRtpPacketClass>(env);
        const auto headerClass = JavaClasses::get<JavaRTCRtpHeaderClass>(env);

        JavaObject obj(env, jPacket);
        jobject jHeader = obj.getObject(packetClass->header);
        JavaObject headerObj(env, jHeader);

        packet.SetPayloadType(headerObj.getInt(headerClass->payloadType));
        packet.SetSequenceNumber(headerObj.getInt(headerClass->sequenceNumber));
        packet.SetTimestamp(headerObj.getLong(headerClass->timestamp));
        // SSRC usually shouldn't be changed here as it breaks the mapping.

        jobject jPayload = obj.getObject(packetClass->payload);
        uint8_t* data = static_cast<uint8_t*>(env->GetDirectBufferAddress(jPayload));
        jlong capacity = env->GetDirectBufferCapacity(jPayload);

        // Update payload if it's different or size changed
        // This is tricky because webrtc::RtpPacket manages its own buffer.
        // We'll assume the direct buffer points to the same memory if it's based on it.
        // If not, we might need a packet.SetPayload(data, size) call.
        if (data != packet.payload().data() || capacity != static_cast<jlong>(packet.payload().size())) {
            packet.SetPayload(rtc::MakeArrayView(data, capacity));
        }
    }

} // namespace RTCRtpPacket
} // namespace jni
