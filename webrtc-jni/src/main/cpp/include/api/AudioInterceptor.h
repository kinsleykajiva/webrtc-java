#ifndef JNI_AUDIO_INTERCEPTOR_H_
#define JNI_AUDIO_INTERCEPTOR_H_

#include "api/interceptor/interceptor.h"
#include "api/rtp_packet.h"
#include "rtc_base/synchronization/mutex.h"
#include <jni.h>
#include <map>
#include <memory>
#include <vector>

namespace jni {

class AudioInterceptor : public webrtc::InterceptorInterface {
public:
    AudioInterceptor(JNIEnv* env, bool enable);
    ~AudioInterceptor() override;

    void SetPeerConnection(JNIEnv* env, jobject java_pc);

    // InterceptorInterface implementation
    void OnRtpConfig(const webrtc::RtpConfig& config) override;
    
    std::unique_ptr<webrtc::RtpPacketInterceptorInterface> CreateRtpPacketInterceptor(
        const webrtc::InterceptorConfig& config) override;

private:
    class RtpPacketInterceptor : public webrtc::RtpPacketInterceptorInterface {
    public:
        RtpPacketInterceptor(AudioInterceptor* parent);
        
        void OnBeforeSendRtp(webrtc::RtpPacket& packet) override;
        void OnReceivedRtp(webrtc::RtpPacket& packet) override;

    private:
        AudioInterceptor* parent_;
    };

    bool IsAudioSSRC(uint32_t ssrc);

    bool enabled_;
    webrtc::Mutex mutex_;
    std::map<uint32_t, bool> ssrc_is_audio_ RTC_GUARDED_BY(mutex_);
};

class AudioInterceptorFactory : public webrtc::InterceptorFactoryInterface {
public:
    AudioInterceptorFactory(JNIEnv* env, bool enable);
    
    std::unique_ptr<webrtc::InterceptorInterface> CreateInterceptor(
        const webrtc::InterceptorConfig& config) override;

private:
    bool enabled_;
};

AudioInterceptor* GetLastCreatedInterceptor();

} // namespace jni

#endif // JNI_AUDIO_INTERCEPTOR_H_
