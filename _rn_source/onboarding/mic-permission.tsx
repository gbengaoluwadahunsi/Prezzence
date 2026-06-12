import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React, { useState } from 'react';
import {
    ActivityIndicator,
    Pressable,
    ScrollView,
    StyleSheet,
    Text,
    View,
    Platform,
    PermissionsAndroid,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { createSession } from '../../src/api/sessions';
import { trackEvent } from '../../src/services/analytics';
import { useSessionStore } from '../../src/store/sessionStore';
import { useAuthStore } from '../../src/store/authStore';
import { useSettingsStore } from '../../src/store/settingsStore';
import { useTranslation } from '../../src/hooks/useTranslation';
import { BETA_UNLOCK_ALL_FEATURES } from '../../src/config/featureFlags';

import { colors, spacing, typography, radii } from '../../src/theme';
import { getPersonasForInterview } from '../../src/data/personas';

const consentCopy: Record<string, {
    title: string;
    body: string;
    audio: string;
    ai: string;
    storage: string;
    accept: string;
    required: string;
    button: string;
    decline: string;
    headline: string;
    subheadline: string;
    privacyTitle: string;
    privacyText: string;
    securityTitle: string;
    securityText: string;
}> = {
    en: {
        title: 'Audio and AI consent',
        body: 'Prezzence uses your microphone audio to transcribe answers, score interview quality, and generate coaching feedback.',
        audio: 'Your voice is recorded only during active answer time.',
        ai: 'Audio may be sent to trusted AI services for transcription and scoring.',
        storage: 'Transcripts, scores, and session feedback are saved to your account history.',
        accept: 'I agree to audio recording and AI analysis for interview practice.',
        required: 'Accept audio and AI consent before starting.',
        button: 'Grant Access & Begin',
        decline: 'Decline Review',
        headline: 'Microphone Permission',
        subheadline: 'To analyze your voice and provide feedback, we need access to your microphone.',
        privacyTitle: 'Session Privacy',
        privacyText: 'Recording starts only when you answer an interview question.',
        securityTitle: 'Protected Processing',
        securityText: 'Your practice data is used to create your own interview feedback.',
    },
    es: {
        title: 'Consentimiento de audio e IA',
        body: 'Prezzence usa el audio del micrófono para transcribir respuestas, puntuar la entrevista y generar recomendaciones.',
        audio: 'Tu voz se graba solo mientras respondes.',
        ai: 'El audio puede enviarse a servicios de IA confiables para transcripción y puntuación.',
        storage: 'Las transcripciones, puntuaciones y comentarios se guardan en tu historial.',
        accept: 'Acepto la grabación de audio y el análisis con IA para practicar entrevistas.',
        required: 'Acepta el consentimiento de audio e IA antes de empezar.',
        button: 'Permitir y empezar',
        decline: 'Revisar después',
        headline: 'Permiso de micrófono',
        subheadline: 'Para analizar tu voz y darte comentarios, necesitamos acceso al micrófono.',
        privacyTitle: 'Privacidad de sesión',
        privacyText: 'La grabación empieza solo cuando respondes una pregunta.',
        securityTitle: 'Procesamiento protegido',
        securityText: 'Tus datos de práctica se usan para crear tus propios comentarios.',
    },
    fr: {
        title: 'Consentement audio et IA',
        body: 'Prezzence utilise votre audio pour transcrire les réponses, évaluer l’entretien et générer du coaching.',
        audio: 'Votre voix est enregistrée uniquement pendant vos réponses.',
        ai: 'L’audio peut être envoyé à des services IA fiables pour transcription et notation.',
        storage: 'Les transcriptions, scores et retours sont enregistrés dans votre historique.',
        accept: 'J’accepte l’enregistrement audio et l’analyse IA pour l’entraînement.',
        required: 'Acceptez le consentement audio et IA avant de commencer.',
        button: 'Autoriser et commencer',
        decline: 'Revoir plus tard',
        headline: 'Autorisation du microphone',
        subheadline: 'Pour analyser votre voix et fournir un retour, nous avons besoin du microphone.',
        privacyTitle: 'Confidentialité de session',
        privacyText: 'L’enregistrement commence seulement quand vous répondez.',
        securityTitle: 'Traitement protégé',
        securityText: 'Vos données servent à créer votre propre feedback.',
    },
    de: {
        title: 'Audio- und KI-Zustimmung',
        body: 'Prezzence nutzt Mikrofon-Audio, um Antworten zu transkribieren, die Interviewqualität zu bewerten und Feedback zu geben.',
        audio: 'Ihre Stimme wird nur während aktiver Antworten aufgenommen.',
        ai: 'Audio kann zur Transkription und Bewertung an vertrauenswürdige KI-Dienste gesendet werden.',
        storage: 'Transkripte, Scores und Feedback werden in Ihrem Verlauf gespeichert.',
        accept: 'Ich stimme Audioaufnahme und KI-Analyse für Interviewtraining zu.',
        required: 'Bitte stimmen Sie Audio und KI zu, bevor Sie starten.',
        button: 'Zugriff erlauben und starten',
        decline: 'Später prüfen',
        headline: 'Mikrofonberechtigung',
        subheadline: 'Für Sprachanalyse und Feedback benötigen wir Zugriff auf Ihr Mikrofon.',
        privacyTitle: 'Sitzungsprivatsphäre',
        privacyText: 'Die Aufnahme startet nur, wenn Sie eine Frage beantworten.',
        securityTitle: 'Geschützte Verarbeitung',
        securityText: 'Ihre Übungsdaten werden für Ihr eigenes Feedback verwendet.',
    },
    it: {
        title: 'Consenso audio e IA',
        body: 'Prezzence usa l’audio del microfono per trascrivere risposte, valutare il colloquio e generare feedback.',
        audio: 'La voce viene registrata solo mentre rispondi.',
        ai: 'L’audio può essere inviato a servizi IA affidabili per trascrizione e valutazione.',
        storage: 'Trascrizioni, punteggi e feedback vengono salvati nella cronologia.',
        accept: 'Accetto registrazione audio e analisi IA per la pratica dei colloqui.',
        required: 'Accetta il consenso audio e IA prima di iniziare.',
        button: 'Consenti e inizia',
        decline: 'Rivedi dopo',
        headline: 'Autorizzazione microfono',
        subheadline: 'Per analizzare la voce e dare feedback serve accesso al microfono.',
        privacyTitle: 'Privacy sessione',
        privacyText: 'La registrazione parte solo quando rispondi a una domanda.',
        securityTitle: 'Elaborazione protetta',
        securityText: 'I tuoi dati servono a creare il tuo feedback.',
    },
    pt: {
        title: 'Consentimento de áudio e IA',
        body: 'O Prezzence usa o áudio do microfone para transcrever respostas, pontuar entrevistas e gerar feedback.',
        audio: 'Sua voz é gravada apenas durante as respostas.',
        ai: 'O áudio pode ser enviado a serviços confiáveis de IA para transcrição e pontuação.',
        storage: 'Transcrições, notas e feedback ficam salvos no histórico.',
        accept: 'Aceito gravação de áudio e análise por IA para prática de entrevistas.',
        required: 'Aceite o consentimento de áudio e IA antes de começar.',
        button: 'Permitir e começar',
        decline: 'Revisar depois',
        headline: 'Permissão de microfone',
        subheadline: 'Para analisar sua voz e dar feedback, precisamos acessar o microfone.',
        privacyTitle: 'Privacidade da sessão',
        privacyText: 'A gravação começa apenas quando você responde.',
        securityTitle: 'Processamento protegido',
        securityText: 'Seus dados de prática geram seu próprio feedback.',
    },
    zh: {
        title: '音频与 AI 同意',
        body: 'Prezzence 使用麦克风音频转写回答、评估面试质量并生成辅导反馈。',
        audio: '只在你回答问题时录制语音。',
        ai: '音频可能发送到可信 AI 服务用于转写和评分。',
        storage: '转写、分数和反馈会保存到你的账号历史。',
        accept: '我同意为面试练习录音并进行 AI 分析。',
        required: '开始前请先同意音频与 AI 分析。',
        button: '授权并开始',
        decline: '稍后查看',
        headline: '麦克风权限',
        subheadline: '为了分析你的声音并提供反馈，我们需要访问麦克风。',
        privacyTitle: '会话隐私',
        privacyText: '只有在你回答面试问题时才开始录音。',
        securityTitle: '受保护处理',
        securityText: '练习数据用于生成你自己的面试反馈。',
    },
    ja: {
        title: '音声とAIへの同意',
        body: 'Prezzenceは音声を文字起こし、面接評価、コーチング生成に使用します。',
        audio: '音声は回答中のみ録音されます。',
        ai: '音声は文字起こしと採点のため信頼できるAIサービスに送信される場合があります。',
        storage: '文字起こし、スコア、フィードバックは履歴に保存されます。',
        accept: '面接練習のための録音とAI分析に同意します。',
        required: '開始前に音声とAI分析への同意が必要です。',
        button: '許可して開始',
        decline: '後で確認',
        headline: 'マイクの許可',
        subheadline: '音声を分析してフィードバックするため、マイクへのアクセスが必要です。',
        privacyTitle: 'セッションのプライバシー',
        privacyText: '録音は質問に回答するときだけ開始されます。',
        securityTitle: '保護された処理',
        securityText: '練習データはあなた自身のフィードバック作成に使われます。',
    },
    ko: {
        title: '오디오 및 AI 동의',
        body: 'Prezzence는 마이크 오디오로 답변을 전사하고 면접 품질을 평가하며 코칭 피드백을 만듭니다.',
        audio: '음성은 답변 중에만 녹음됩니다.',
        ai: '오디오는 전사와 채점을 위해 신뢰할 수 있는 AI 서비스로 전송될 수 있습니다.',
        storage: '전사, 점수, 피드백은 계정 기록에 저장됩니다.',
        accept: '면접 연습을 위한 오디오 녹음과 AI 분석에 동의합니다.',
        required: '시작하기 전에 오디오 및 AI 동의가 필요합니다.',
        button: '허용하고 시작',
        decline: '나중에 검토',
        headline: '마이크 권한',
        subheadline: '음성을 분석하고 피드백을 제공하려면 마이크 접근이 필요합니다.',
        privacyTitle: '세션 개인정보',
        privacyText: '녹음은 질문에 답할 때만 시작됩니다.',
        securityTitle: '보호된 처리',
        securityText: '연습 데이터는 개인 피드백 생성에 사용됩니다.',
    },
    ar: {
        title: 'موافقة الصوت والذكاء الاصطناعي',
        body: 'يستخدم Prezzence صوت الميكروفون لتفريغ الإجابات وتقييم المقابلة وإنشاء ملاحظات تدريبية.',
        audio: 'يتم تسجيل صوتك فقط أثناء الإجابة النشطة.',
        ai: 'قد يتم إرسال الصوت إلى خدمات ذكاء اصطناعي موثوقة للتفريغ والتقييم.',
        storage: 'يتم حفظ النصوص والدرجات والملاحظات في سجل حسابك.',
        accept: 'أوافق على تسجيل الصوت وتحليله بالذكاء الاصطناعي للتدريب على المقابلات.',
        required: 'اقبل موافقة الصوت والذكاء الاصطناعي قبل البدء.',
        button: 'السماح والبدء',
        decline: 'المراجعة لاحقاً',
        headline: 'إذن الميكروفون',
        subheadline: 'لتحليل صوتك وتقديم الملاحظات، نحتاج إلى الوصول إلى الميكروفون.',
        privacyTitle: 'خصوصية الجلسة',
        privacyText: 'يبدأ التسجيل فقط عند إجابتك عن سؤال.',
        securityTitle: 'معالجة محمية',
        securityText: 'تُستخدم بيانات التدريب لإنشاء ملاحظاتك الخاصة.',
    },
    hi: {
        title: 'ऑडियो और AI सहमति',
        body: 'Prezzence आपके माइक्रोफ़ोन ऑडियो से उत्तरों को ट्रांसक्राइब, स्कोर और कोचिंग फीडबैक बनाता है।',
        audio: 'आपकी आवाज़ केवल उत्तर देते समय रिकॉर्ड होती है।',
        ai: 'ट्रांसक्रिप्शन और स्कोरिंग के लिए ऑडियो भरोसेमंद AI सेवाओं को भेजा जा सकता है।',
        storage: 'ट्रांसक्रिप्ट, स्कोर और फीडबैक आपके खाते के इतिहास में सहेजे जाते हैं।',
        accept: 'मैं इंटरव्यू अभ्यास के लिए ऑडियो रिकॉर्डिंग और AI विश्लेषण से सहमत हूं।',
        required: 'शुरू करने से पहले ऑडियो और AI सहमति स्वीकार करें।',
        button: 'अनुमति दें और शुरू करें',
        decline: 'बाद में देखें',
        headline: 'माइक्रोफ़ोन अनुमति',
        subheadline: 'आपकी आवाज़ का विश्लेषण और फीडबैक देने के लिए माइक्रोफ़ोन की आवश्यकता है।',
        privacyTitle: 'सेशन गोपनीयता',
        privacyText: 'रिकॉर्डिंग केवल तब शुरू होती है जब आप प्रश्न का उत्तर देते हैं।',
        securityTitle: 'सुरक्षित प्रोसेसिंग',
        securityText: 'आपके अभ्यास डेटा से आपका अपना फीडबैक बनाया जाता है।',
    },
};

export default function MicPermissionScreen() {
    const [loading, setLoading] = useState(false);
    const { t } = useTranslation();
    const {
        onboardingCompleted,
        setOnboardingCompleted,
        setOnboardingStep,
        audioAiConsentAccepted,
        setAudioAiConsentAccepted
    } = useSettingsStore();
    const [consentChecked, setConsentChecked] = useState(audioAiConsentAccepted);
    const [consentError, setConsentError] = useState<string | null>(null);

    React.useEffect(() => {
        setOnboardingStep('/onboarding/mic-permission');
    }, []);

    React.useEffect(() => {
        setConsentChecked(audioAiConsentAccepted);
    }, [audioAiConsentAccepted]);

    const {
        roleTitle,
        industry,
        seniority,
        interviewType,
        companyName,
        companyWebsite,
        companyContext,
        enableWebResearch,
        isPremium,
        difficulty,
        length,
        language,
        setSessionPreparing,
        setSessionResult
    } = useSessionStore();
    const hasPremiumAccess = BETA_UNLOCK_ALL_FEATURES || isPremium;
    const copy = consentCopy[language] || consentCopy.en;

    const handleGrantAccess = async () => {
        let navigatedToEntering = false;
        if (!audioAiConsentAccepted && !consentChecked) {
            setConsentError(copy.required);
            return;
        }

        setConsentError(null);
        setAudioAiConsentAccepted(true);
        setLoading(true);
        try {
            let granted = false;
            if (Platform.OS === 'android') {
                const result = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.RECORD_AUDIO);
                granted = result === PermissionsAndroid.RESULTS.GRANTED;
            } else {
                const { requestRecordingPermissionsAsync } = require('expo-audio');
                const permission = await requestRecordingPermissionsAsync();
                granted = !!permission.granted;
            }

            if (!granted) {
                setLoading(false);
                router.replace('/errors/microphone-denied' as any);
                return;
            }

            const sessionState = useSessionStore.getState();
            const sessionLanguage = sessionState.language || 'en';
            const selectedPanel = sessionState.selectedPersonas.length > 0
                ? sessionState.selectedPersonas
                : getPersonasForInterview(
                    sessionState.interviewerMode,
                    sessionState.interviewerPersonality,
                    sessionState.industry
                );
            if (sessionState.selectedPersonas.length === 0) {
                sessionState.setSelectedPersonas(selectedPanel);
            }
            const panelConfig = selectedPanel.map((p, i) => ({
                persona_id: p.id,
                seat: i === 0 ? 'left' : i === 1 ? 'centre' : 'right'
            }));

            const sessionConfig = {
                role_title: roleTitle || 'Candidate',
                industry: industry || 'General Business',
                seniority: seniority?.toLowerCase() || 'mid',
                interview_type: interviewType || 'job',
                difficulty: difficulty?.toLowerCase() || 'medium',
                include_technical: useSessionStore.getState().isTechnical,
                length: length || 'standard',
                panel_config: panelConfig,
                company_name: companyName,
                company_website: companyWebsite,
                company_context: companyContext,
                enable_web_research: Boolean(enableWebResearch && hasPremiumAccess),
                language: sessionLanguage
            };

            setSessionPreparing();
            setOnboardingCompleted(true);
            router.replace('/interview/entering' as any);
            navigatedToEntering = true;

            // Create the session after navigation so permission success feels instant.
            // The entering screen already shows a preparing state until questions arrive.
            let session = useAuthStore.getState().session;
            if (!session?.access_token) {
                console.warn('[MicPermission] Session missing, waiting for auth sync...');
                for (let i = 0; i < 10; i++) {
                    await new Promise(resolve => setTimeout(resolve, 200));
                    session = useAuthStore.getState().session;
                    if (session?.access_token) break;
                }
            }

            if (!session?.access_token) {
                console.error('[MicPermission] Critical: No session found after waiting');
                throw new Error('Authentication required');
            }

            let result;
            try {
                result = await createSession(sessionConfig);
            } catch (error: any) {
                if (error.response?.status !== 401) throw error;

                console.warn('[MicPermission] 401 detected, attempting session refresh...');
                const { refreshSession } = useAuthStore.getState();
                const refreshed = await refreshSession();
                if (!refreshed) {
                    throw new Error('Session expired. Please sign in again.');
                }

                console.log('[MicPermission] Retrying session creation...');
                result = await createSession(sessionConfig);
            }

            setSessionResult(result.session_id, result.questions);
            trackEvent('session_created', {
                sessionId: result.session_id,
                questionCount: result.questions?.length || 0,
                language: sessionLanguage,
                difficulty,
                length,
            });
        } catch (error: any) {
            const status = error.response?.status;
            const data = error.response?.data;
            const detailPayload = data?.detail;
            const detailCode = typeof detailPayload === 'object' && detailPayload
                ? String(detailPayload.code || '')
                : '';
            const detail = typeof detailPayload === 'string'
                ? detailPayload
                : String(detailPayload?.message || error?.message || '');
            if (status === 402 && (detailCode === 'premium_required' || /premium|upgrade|free plan/i.test(detail))) {
                console.warn('[MicPermission] Premium required for session creation:', detail);
                router.push('/(modals)/paywall' as any);
                return;
            }
            console.warn(`[MicPermission] Session creation failed (${status || 'Network Error'}):`, data || error.message);
            if (status === 403 && /device limit/i.test(detail)) {
                useAuthStore.getState().signOut();
                router.replace({
                    pathname: '/auth/sign-in',
                    params: {
                        error: 'Session reset needed. Please sign in again.'
                    }
                } as any);
                return;
            }
            if (String(error?.message || '').toLowerCase().includes('sign in again')) {
                router.replace('/auth/sign-in' as any);
                return;
            }

            const message = String(error?.message || detail || '').toLowerCase();
            if (message.includes('timed out') || message.includes('network') || !status) {
                if (navigatedToEntering) {
                    router.replace('/errors/network-error' as any);
                    return;
                }
                setConsentError(
                    'Your interview is taking longer than expected to prepare. Please wait a few seconds and tap Grant Access again.'
                );
                return;
            }

            router.replace('/errors/network-error' as any);
        } finally {
            setLoading(false);
        }
    };

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Pressable
                    onPress={() => {
                        const { user, session } = useAuthStore.getState();
                        router.replace(user || session ? '/(tabs)/home' as any : '/' as any);
                    }}
                    style={styles.backBtn}
                >
                    <Feather name="chevron-left" size={24} color={colors.accent} />
                </Pressable>
                <View style={styles.headerCenter}>
                    <Text style={styles.stepText}>
                        READY TO BEGIN
                    </Text>
                    <Text style={styles.headerTitle}>{t.onboarding.permissionsTitle}</Text>
                </View>
                <View style={styles.headerSpacer} />
            </View>

            <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
                <View style={styles.iconCircle}>
                    <View style={styles.iconBackground}>
                        <Feather name="mic" size={56} color={colors.accent} />
                    </View>
                    <View style={styles.ripple1} />
                    <View style={styles.ripple2} />
                    <View style={styles.ripple3} />
                </View>

                <Text style={styles.headline}>{copy.headline || t.onboarding.permissionsHeadline}</Text>
                <Text style={styles.subheadline}>
                    {copy.subheadline}
                </Text>

                <View style={styles.infoBox}>
                    <View style={styles.infoRow}>
                        <View style={styles.miniIcon}>
                            <Feather name="shield" size={16} color={colors.accent} />
                        </View>
                        <View style={styles.infoCopy}>
                            <Text style={styles.infoTitle}>{copy.privacyTitle}</Text>
                            <Text style={styles.infoText}>{copy.privacyText}</Text>
                        </View>
                    </View>
                    <View style={styles.infoRow}>
                        <View style={styles.miniIcon}>
                            <Feather name="lock" size={16} color={colors.accent} />
                        </View>
                        <View style={styles.infoCopy}>
                            <Text style={styles.infoTitle}>{copy.securityTitle}</Text>
                            <Text style={styles.infoText}>{copy.securityText}</Text>
                        </View>
                    </View>
                </View>

                <View style={styles.consentBox}>
                    <Text style={styles.consentTitle}>{copy.title}</Text>
                    <Text style={styles.consentBody}>{copy.body}</Text>
                    <View style={styles.consentBullet}>
                        <Feather name="mic" size={15} color={colors.accent} />
                        <Text style={styles.consentBulletText}>{copy.audio}</Text>
                    </View>
                    <View style={styles.consentBullet}>
                        <Feather name="cpu" size={15} color={colors.accent} />
                        <Text style={styles.consentBulletText}>{copy.ai}</Text>
                    </View>
                    <View style={styles.consentBullet}>
                        <Feather name="database" size={15} color={colors.accent} />
                        <Text style={styles.consentBulletText}>{copy.storage}</Text>
                    </View>
                </View>
            </ScrollView>

            <View style={styles.footer}>
                <Pressable
                    onPress={() => {
                        const next = !consentChecked;
                        setConsentChecked(next);
                        if (next) setConsentError(null);
                    }}
                    style={styles.footerConsentToggle}
                >
                    <View style={[styles.checkbox, consentChecked && styles.checkboxChecked]}>
                        {consentChecked && <Feather name="check" size={16} color={colors.text} />}
                    </View>
                    <Text style={styles.consentAccept} numberOfLines={3}>{copy.accept}</Text>
                </Pressable>
                {consentError ? <Text style={styles.consentError}>{consentError}</Text> : null}
                <Pressable
                    onPress={handleGrantAccess}
                    disabled={loading || !consentChecked}
                    style={({ pressed }) => [
                        styles.primaryButton,
                        !consentChecked && styles.primaryButtonDisabled,
                        (pressed || loading) && { opacity: 0.9 }
                    ]}
                >
                    {loading ? (
                        <ActivityIndicator color={colors.text} />
                    ) : (
                        <>
                            <Text style={styles.buttonText}>{copy.button}</Text>
                            <Feather name="zap" size={20} color={colors.text} style={styles.buttonIcon} />
                        </>
                    )}
                </Pressable>
                <Pressable
                    onPress={() => router.back()}
                    style={styles.ghostButton}
                >
                    <Text style={styles.ghostText}>{copy.decline}</Text>
                </Pressable>
            </View>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: colors.background,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        height: 80,
    },
    backBtn: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: colors.card,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: colors.glassLight,
    },
    headerCenter: {
        alignItems: 'center',
    },
    headerSpacer: {
        width: 44,
    },
    headerTitle: {
        color: colors.text,
        fontSize: 16,
        fontWeight: '800',
    },
    stepText: {
        color: colors.accent,
        fontSize: 10,
        fontWeight: '900',
        letterSpacing: 2,
        marginBottom: 4,
        textTransform: 'uppercase',
    },
    content: {
        flexGrow: 1,
        alignItems: 'center',
        justifyContent: 'flex-start',
        paddingHorizontal: 24,
        paddingBottom: 16,
    },
    iconCircle: {
        width: 156,
        height: 156,
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 18,
        marginTop: 6,
    },
    iconBackground: {
        width: 88,
        height: 88,
        borderRadius: 44,
        backgroundColor: colors.card,
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 10,
        borderWidth: 1,
        borderColor: colors.glassMedium,
        shadowColor: colors.accent,
        shadowOpacity: 0.2,
        shadowRadius: 20,
    },
    ripple1: {
        position: 'absolute',
        width: 116,
        height: 116,
        borderRadius: 58,
        borderWidth: 1,
        borderColor: 'rgba(108, 99, 255, 0.2)',
    },
    ripple2: {
        position: 'absolute',
        width: 136,
        height: 136,
        borderRadius: 68,
        borderWidth: 1,
        borderColor: 'rgba(108, 99, 255, 0.1)',
    },
    ripple3: {
        position: 'absolute',
        width: 156,
        height: 156,
        borderRadius: 78,
        borderWidth: 1,
        borderColor: 'rgba(108, 99, 255, 0.05)',
    },
    headline: {
        color: colors.text,
        fontSize: 32,
        fontWeight: '900',
        textAlign: 'center',
        lineHeight: 40,
        letterSpacing: -1,
        marginBottom: 10,
    },
    subheadline: {
        color: colors.textMuted,
        fontSize: 16,
        textAlign: 'center',
        lineHeight: 24,
        marginBottom: 18,
        opacity: 0.8,
    },
    infoBox: {
        backgroundColor: colors.card,
        borderRadius: 32,
        padding: 18,
        width: '100%',
        gap: 20,
        borderWidth: 1,
        borderColor: colors.glassLight,
    },
    infoRow: {
        flexDirection: 'row',
        gap: 16,
    },
    infoCopy: {
        flex: 1,
    },
    miniIcon: {
        width: 36,
        height: 36,
        borderRadius: 12,
        backgroundColor: colors.accentGlass,
        alignItems: 'center',
        justifyContent: 'center',
    },
    infoTitle: {
        color: colors.text,
        fontSize: 15,
        fontWeight: '800',
        marginBottom: 2,
    },
    infoText: {
        color: colors.textMuted,
        fontSize: 13,
        lineHeight: 18,
    },
    consentBox: {
        backgroundColor: colors.card,
        borderRadius: 24,
        padding: 20,
        width: '100%',
        gap: 12,
        borderWidth: 1,
        borderColor: colors.glassMedium,
        marginTop: 18,
    },
    consentTitle: {
        color: colors.text,
        fontSize: 18,
        fontWeight: '900',
    },
    consentBody: {
        color: colors.textMuted,
        fontSize: 13,
        lineHeight: 19,
    },
    consentBullet: {
        flexDirection: 'row',
        gap: 10,
        alignItems: 'flex-start',
    },
    consentBulletText: {
        color: colors.text,
        fontSize: 13,
        lineHeight: 18,
        flex: 1,
    },
    consentToggle: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
        paddingTop: 6,
    },
    footerConsentToggle: {
        minHeight: 54,
        borderRadius: 18,
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.glassMedium,
        paddingHorizontal: 14,
        paddingVertical: 10,
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
    },
    checkbox: {
        width: 24,
        height: 24,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: colors.glassMedium,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: colors.background,
    },
    checkboxChecked: {
        backgroundColor: colors.accent,
        borderColor: colors.accent,
    },
    consentAccept: {
        color: colors.text,
        fontSize: 13,
        lineHeight: 18,
        fontWeight: '700',
        flex: 1,
    },
    consentError: {
        color: '#FF5C7A',
        fontSize: 12,
        fontWeight: '700',
    },
    footer: {
        paddingHorizontal: 20,
        paddingTop: 12,
        paddingBottom: 24,
        gap: 10,
        backgroundColor: colors.background,
    },
    primaryButton: {
        backgroundColor: colors.accent,
        height: 64,
        borderRadius: 32,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: colors.accent,
        shadowOpacity: 0.4,
        shadowRadius: 15,
        elevation: 10,
    },
    primaryButtonDisabled: {
        opacity: 0.45,
    },
    buttonText: {
        color: colors.text,
        fontSize: 18,
        fontWeight: '800',
    },
    buttonIcon: {
        marginLeft: 10,
    },
    ghostButton: {
        height: 56,
        alignItems: 'center',
        justifyContent: 'center',
    },
    ghostText: {
        color: colors.textMuted,
        fontSize: 16,
        fontWeight: '600',
    },
});
