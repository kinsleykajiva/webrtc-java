#include "api/AudioInterceptor.h"
#include "api/RTCRtpPacket.h"
#include "JavaRef.h"
#include "JavaUtils.h"
#include "JNI_RTCPeerConnection.h"
#include "JNI_WebRTC.h"
#include "rtc_base/logging.h"

#include <map>
#include <mutex>

namespace jni {

// Global mapping from native PC to Java PC Global Ref
// This is necessary because Interceptors are created deep within WebRTC
static std::mutex g_pc_map_mutex;
static std::map<void*, jobject> g_pc_map;

void RegisterPeerConnection(void* native_pc, jobject java_pc) {
    std::lock_guard<std::mutex> lock(g_pc_map_mutex);
    g_pc_map[native_pc] = java_pc;
}

void UnregisterPeerConnection(void* native_pc) {
    std::lock_guard<std::mutex> lock(g_pc_map_mutex);
    g_pc_map.erase(native_pc);
}

jobject GetJavaPeerConnection(void* native_pc) {
    std::lock_guard<std::mutex> lock(g_pc_map_mutex);
    auto it = g_pc_map.find(native_pc);
    if (it != g_pc_map.end()) {
        return it->second;
    }
    return nullptr;
}


// Thread-local to capture the last created interceptor during CreatePeerConnection
thread_local AudioInterceptor* g_last_interceptor = nullptr;

AudioInterceptor* GetLastCreatedInterceptor() {
    return g_last_interceptor;
}


AudioInterceptor::AudioInterceptor(JNIEnv* env, bool enable)
    : enabled_(enable), java_pc_(nullptr) {}

AudioInterceptor::~AudioInterceptor() {
    if (java_pc_) {
        JNIEnv* env = AttachCurrentThread();
        env->DeleteGlobalRef(java_pc_);
    }
}

void AudioInterceptor::SetPeerConnection(JNIEnv* env, jobject java_pc) {
    webrtc::MutexLock lock(&mutex_);
    if (java_pc_) {
        env->DeleteGlobalRef(java_pc_);
    }
    java_pc_ = env->NewGlobalRef(java_pc);
}

void AudioInterceptor::OnRtpConfig(const webrtc::RtpConfig& config) {
    webrtc::MutexLock lock(&mutex_);
    for (const auto& ssrc : config.ssrcs) {
        ssrc_is_audio_[ssrc] = true; 
    }
}

std::unique_ptr<webrtc::RtpPacketInterceptorInterface> AudioInterceptor::CreateRtpPacketInterceptor(
    const webrtc::InterceptorConfig& config) {
    return std::make_unique<RtpPacketInterceptor>(this);
}

bool AudioInterceptor::IsAudioSSRC(uint32_t ssrc) {
    webrtc::MutexLock lock(&mutex_);
    return ssrc_is_audio_.count(ssrc) > 0;
}


AudioInterceptor::RtpPacketInterceptor::RtpPacketInterceptor(AudioInterceptor* parent)
    : parent_(parent) {}

void AudioInterceptor::RtpPacketInterceptor::OnBeforeSendRtp(webrtc::RtpPacket& packet) {
    if (!parent_->enabled_) return;
    if (!parent_->IsAudioSSRC(packet.Ssrc())) return;

    JNIEnv* env = AttachCurrentThread();
    
    webrtc::MutexLock lock(&parent_->mutex_);
    if (!parent_->java_pc_) return;

    // Dispatch to Java
    // In dev.onvoid.webrtc.RTCPeerConnection:
    // private RTCRtpPacket interceptRTP(RTCRtpPacket packet, RTCRtpSender sender)
    
    jclass pc_class = env->GetObjectClass(parent_->java_pc_);
    jmethodID intercept_method = env->GetMethodID(pc_class, "interceptRTP", 
        "(L" PKG "RTCRtpPacket;L" PKG "RTCRtpSender;)L" PKG "RTCRtpPacket;");

    if (!intercept_method) return;

    JavaLocalRef<jobject> jPacket = RTCRtpPacket::toJava(env, packet);
    
    // For now sender is null unless we implement better tracking
    jobject intercepted = env->CallObjectMethod(parent_->java_pc_, intercept_method, jPacket.get(), nullptr);
    
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return;
    }

    if (!intercepted) {
        // Drop packet by zeroing it or similar? WebRTC InterceptorInterface 
        // doesn't have a clear "drop" mechanism here easily, but we can 
        // potentially clear the payload.
        packet.SetPayloadSize(0);
    } else {
        RTCRtpPacket::toNative(env, intercepted, packet);
    }
}

void AudioInterceptor::RtpPacketInterceptor::OnReceivedRtp(webrtc::RtpPacket& packet) {
    if (!parent_->enabled_) return;
    if (!parent_->IsAudioSSRC(packet.Ssrc())) return;
    
    JNIEnv* env = AttachCurrentThread();
    
    webrtc::MutexLock lock(&parent_->mutex_);
    if (!parent_->java_pc_) return;

    jclass pc_class = env->GetObjectClass(parent_->java_pc_);
    jmethodID intercept_method = env->GetMethodID(pc_class, "interceptIncomingRTP", 
        "(L" PKG "RTCRtpPacket;L" PKG "RTCRtpReceiver;)L" PKG "RTCRtpPacket;");

    if (!intercept_method) return;

    JavaLocalRef<jobject> jPacket = RTCRtpPacket::toJava(env, packet);
    jobject intercepted = env->CallObjectMethod(parent_->java_pc_, intercept_method, jPacket.get(), nullptr);
    
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return;
    }

    if (!intercepted) {
        packet.SetPayloadSize(0);
    } else {
        RTCRtpPacket::toNative(env, intercepted, packet);
    }
}


AudioInterceptorFactory::AudioInterceptorFactory(JNIEnv* env, bool enable)
    : enabled_(enable) {}

std::unique_ptr<webrtc::InterceptorInterface> AudioInterceptorFactory::CreateInterceptor(
    const webrtc::InterceptorConfig& config) {
    auto interceptor = std::make_unique<AudioInterceptor>(AttachCurrentThread(), enabled_);
    g_last_interceptor = interceptor.get();
    return interceptor;
}

} // namespace jni
