import { Feather, MaterialCommunityIcons } from '@expo/vector-icons';
import * as DocumentPicker from 'expo-document-picker';
import { router } from 'expo-router';
import React from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import {
  deleteResumeProfile,
  getResumeProfile,
  getUserProgress,
  ProgressData,
  ResumeProfile,
  uploadResumeProfile,
} from '../../src/api/users';
import { useTranslation } from '../../src/hooks/useTranslation';
import { useAuthStore } from '../../src/store/authStore';
import { useSessionStore } from '../../src/store/sessionStore';
import { useSettingsStore } from '../../src/store/settingsStore';
import { requestPresenceCameraPermission } from '../../src/native/PresenceCameraView';

const Colors = {
  bgPrimary: '#0A0A0F',
  accent: '#6C63FF',
  textSecondary: '#8A8A9A',
  card: '#171725',
  border: '#2A2A3E',
  success: '#00D68F',
  danger: '#FF4757',
  warning: '#FFB347',
};

function StatCard({ label, value, unit }: { label: string; value: string | number; unit?: string }) {
  return (
    <View style={styles.statCard}>
      <Text style={styles.statLabel} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>
        {label}
      </Text>
      <View style={styles.statValueRow}>
        <Text style={styles.statValue}>{value}</Text>
        {unit ? <Text style={styles.statUnit}>{unit}</Text> : null}
      </View>
    </View>
  );
}

function MenuLink({
  icon,
  title,
  subtitle,
  value,
  color = Colors.accent,
  onPress,
}: {
  icon: string;
  title: string;
  subtitle: string;
  value?: string;
  color?: string;
  onPress?: () => void;
}) {
  return (
    <Pressable style={styles.menuItem} onPress={onPress}>
      <View style={[styles.menuIconBox, { backgroundColor: color + '16' }]}>
        <Feather name={icon as any} size={21} color={color} />
      </View>
      <View style={styles.menuInfo}>
        <Text style={styles.menuTitle}>{title}</Text>
        <Text style={styles.menuSubtitle} numberOfLines={1}>{subtitle}</Text>
      </View>
      {value ? <Text style={styles.menuValue}>{value}</Text> : null}
      <Feather name="chevron-right" size={18} color={Colors.textSecondary} />
    </Pressable>
  );
}

function languageLabel(language: string) {
  const labels: Record<string, string> = {
    en: 'English',
    es: 'Spanish',
    fr: 'French',
    de: 'German',
    it: 'Italian',
    pt: 'Portuguese',
    zh: 'Chinese',
    ja: 'Japanese',
    ko: 'Korean',
    ar: 'Arabic',
    hi: 'Hindi',
  };
  return labels[language] || language.toUpperCase();
}

function profileCopy(language: string) {
  const en = {
    noEmail: 'No email connected',
    freePlan: 'Free plan',
    proPlan: 'Pro plan',
    loading: 'Loading profile metrics...',
    sessions: 'Sessions',
    avgScore: 'Avg score',
    practice: 'Practice',
    coachingFocus: 'Coaching focus',
    emptyCoaching: 'Complete your first interview to build a coaching profile.',
    bestSkill: 'Best skill',
    notEnoughData: 'Not enough data',
    account: 'Account',
    accountSub: 'Name, email, and practice profile',
    edit: 'Edit',
    subscription: 'Subscription',
    access: 'access',
    active: 'Active',
    upgrade: 'Upgrade',
    language: 'Language',
    change: 'Change',
    notifications: 'Notifications',
    notificationsSub: 'Practice reminders and score alerts',
    privacy: 'Privacy & Security',
    privacySub: 'Data, devices, and deletion controls',
    help: 'Help',
    helpSub: 'Support, scoring, and troubleshooting',
    signOut: 'Sign out',
    signOutSub: 'End this session on this device',
  };
  const copies: Record<string, typeof en> = {
    en,
    es: { ...en, noEmail: 'Sin correo conectado', freePlan: 'Plan gratuito', proPlan: 'Plan Pro', loading: 'Cargando métricas del perfil...', sessions: 'Sesiones', avgScore: 'Puntuación media', practice: 'Práctica', coachingFocus: 'Enfoque de coaching', emptyCoaching: 'Completa tu primera entrevista para crear tu perfil de coaching.', bestSkill: 'Mejor habilidad', notEnoughData: 'Datos insuficientes', account: 'Cuenta', accountSub: 'Nombre, correo e identidad de entrevista', edit: 'Editar', subscription: 'Suscripción', access: 'acceso', active: 'Activa', upgrade: 'Mejorar', language: 'Idioma', change: 'Cambiar', notifications: 'Notificaciones', notificationsSub: 'Recordatorios y alertas de puntuación', privacy: 'Privacidad y seguridad', privacySub: 'Datos, dispositivos y controles de eliminación', help: 'Ayuda', helpSub: 'Soporte, puntuación y solución de problemas', signOut: 'Cerrar sesión', signOutSub: 'Finalizar esta sesión en este dispositivo' },
    fr: { ...en, noEmail: 'Aucun e-mail connecté', freePlan: 'Offre gratuite', proPlan: 'Offre Pro', loading: 'Chargement des métriques...', sessions: 'Séances', avgScore: 'Score moyen', practice: 'Pratique', coachingFocus: 'Focus coaching', emptyCoaching: 'Terminez votre premier entretien pour créer votre profil de coaching.', bestSkill: 'Meilleure compétence', notEnoughData: 'Données insuffisantes', account: 'Compte', accountSub: 'Nom, e-mail et identité d’entretien', edit: 'Modifier', subscription: 'Abonnement', access: 'accès', active: 'Actif', upgrade: 'Améliorer', language: 'Langue', change: 'Changer', notifications: 'Notifications', notificationsSub: 'Rappels et alertes de score', privacy: 'Confidentialité et sécurité', privacySub: 'Données, appareils et suppression', help: 'Aide', helpSub: 'Support, scoring et dépannage', signOut: 'Se déconnecter', signOutSub: 'Terminer cette session sur cet appareil' },
    de: { ...en, noEmail: 'Keine E-Mail verbunden', freePlan: 'Kostenloser Plan', proPlan: 'Pro-Plan', loading: 'Profilmetriken werden geladen...', sessions: 'Sitzungen', avgScore: 'Durchschnitt', practice: 'Praxis', coachingFocus: 'Coaching-Fokus', emptyCoaching: 'Schließen Sie Ihr erstes Interview ab, um ein Coaching-Profil zu erstellen.', bestSkill: 'Stärkste Fähigkeit', notEnoughData: 'Nicht genug Daten', account: 'Konto', accountSub: 'Name, E-Mail und Interviewidentität', edit: 'Bearbeiten', subscription: 'Abo', access: 'Zugang', active: 'Aktiv', upgrade: 'Upgraden', language: 'Sprache', change: 'Ändern', notifications: 'Benachrichtigungen', notificationsSub: 'Erinnerungen und Score-Hinweise', privacy: 'Datenschutz und Sicherheit', privacySub: 'Daten, Geräte und Löschung', help: 'Hilfe', helpSub: 'Support, Bewertung und Fehlerbehebung', signOut: 'Abmelden', signOutSub: 'Diese Sitzung auf dem Gerät beenden' },
    it: { ...en, noEmail: 'Nessuna email collegata', freePlan: 'Piano gratuito', proPlan: 'Piano Pro', loading: 'Caricamento metriche profilo...', sessions: 'Sessioni', avgScore: 'Punteggio medio', practice: 'Pratica', coachingFocus: 'Focus coaching', emptyCoaching: 'Completa il primo colloquio per creare il profilo coaching.', bestSkill: 'Migliore abilità', notEnoughData: 'Dati insufficienti', account: 'Account', accountSub: 'Nome, email e identità colloquio', edit: 'Modifica', subscription: 'Abbonamento', access: 'accesso', active: 'Attivo', upgrade: 'Aggiorna', language: 'Lingua', change: 'Cambia', notifications: 'Notifiche', notificationsSub: 'Promemoria e avvisi punteggio', privacy: 'Privacy e sicurezza', privacySub: 'Dati, dispositivi ed eliminazione', help: 'Aiuto', helpSub: 'Supporto, punteggi e problemi', signOut: 'Esci', signOutSub: 'Termina questa sessione sul dispositivo' },
    pt: { ...en, noEmail: 'Nenhum e-mail conectado', freePlan: 'Plano gratuito', proPlan: 'Plano Pro', loading: 'Carregando métricas...', sessions: 'Sessões', avgScore: 'Pontuação média', practice: 'Prática', coachingFocus: 'Foco de coaching', emptyCoaching: 'Conclua sua primeira entrevista para criar seu perfil de coaching.', bestSkill: 'Melhor habilidade', notEnoughData: 'Dados insuficientes', account: 'Conta', accountSub: 'Nome, e-mail e identidade da entrevista', edit: 'Editar', subscription: 'Assinatura', access: 'acesso', active: 'Ativa', upgrade: 'Atualizar', language: 'Idioma', change: 'Alterar', notifications: 'Notificações', notificationsSub: 'Lembretes e alertas de pontuação', privacy: 'Privacidade e segurança', privacySub: 'Dados, dispositivos e exclusão', help: 'Ajuda', helpSub: 'Suporte, pontuação e solução de problemas', signOut: 'Sair', signOutSub: 'Encerrar esta sessão neste dispositivo' },
    zh: { ...en, noEmail: '未连接邮箱', freePlan: '免费计划', proPlan: 'Pro 计划', loading: '正在加载个人指标...', sessions: '会话', avgScore: '平均分', practice: '练习', coachingFocus: '辅导重点', emptyCoaching: '完成第一次面试后即可生成辅导档案。', bestSkill: '最强技能', notEnoughData: '数据不足', account: '账户', accountSub: '姓名、邮箱和面试身份', edit: '编辑', subscription: '订阅', access: '权限', active: '已启用', upgrade: '升级', language: '语言', change: '更改', notifications: '通知', notificationsSub: '练习提醒和分数提醒', privacy: '隐私与安全', privacySub: '数据、设备和删除控制', help: '帮助', helpSub: '支持、评分和故障排查', signOut: '退出登录', signOutSub: '结束此设备上的会话' },
    ja: { ...en, noEmail: 'メール未接続', freePlan: '無料プラン', proPlan: 'Proプラン', loading: 'プロフィール指標を読み込み中...', sessions: 'セッション', avgScore: '平均スコア', practice: '練習', coachingFocus: 'コーチング重点', emptyCoaching: '最初の面接を完了するとコーチングプロフィールが作成されます。', bestSkill: '最も強いスキル', notEnoughData: 'データ不足', account: 'アカウント', accountSub: '名前、メール、面接ID', edit: '編集', subscription: 'サブスクリプション', access: 'アクセス', active: '有効', upgrade: 'アップグレード', language: '言語', change: '変更', notifications: '通知', notificationsSub: '練習リマインダーとスコア通知', privacy: 'プライバシーとセキュリティ', privacySub: 'データ、デバイス、削除設定', help: 'ヘルプ', helpSub: 'サポート、採点、トラブル対応', signOut: 'サインアウト', signOutSub: 'このデバイスのセッションを終了' },
    ko: { ...en, noEmail: '이메일이 연결되지 않음', freePlan: '무료 플랜', proPlan: 'Pro 플랜', loading: '프로필 지표 로딩 중...', sessions: '세션', avgScore: '평균 점수', practice: '연습', coachingFocus: '코칭 포커스', emptyCoaching: '첫 인터뷰를 완료하면 코칭 프로필이 생성됩니다.', bestSkill: '최고 역량', notEnoughData: '데이터 부족', account: '계정', accountSub: '이름, 이메일, 인터뷰 ID', edit: '편집', subscription: '구독', access: '접근', active: '활성', upgrade: '업그레이드', language: '언어', change: '변경', notifications: '알림', notificationsSub: '연습 리마인더 및 점수 알림', privacy: '개인정보 및 보안', privacySub: '데이터, 기기, 삭제 제어', help: '도움말', helpSub: '지원, 채점, 문제 해결', signOut: '로그아웃', signOutSub: '이 기기의 세션 종료' },
    ar: { ...en, noEmail: 'لا يوجد بريد متصل', freePlan: 'الخطة المجانية', proPlan: 'خطة Pro', loading: 'جار تحميل مقاييس الملف...', sessions: 'الجلسات', avgScore: 'متوسط النتيجة', practice: 'التدريب', coachingFocus: 'تركيز التدريب', emptyCoaching: 'أكمل أول مقابلة لإنشاء ملف التدريب الخاص بك.', bestSkill: 'أفضل مهارة', notEnoughData: 'بيانات غير كافية', account: 'الحساب', accountSub: 'الاسم والبريد وهوية المقابلة', edit: 'تعديل', subscription: 'الاشتراك', access: 'وصول', active: 'نشط', upgrade: 'ترقية', language: 'اللغة', change: 'تغيير', notifications: 'الإشعارات', notificationsSub: 'تذكيرات التدريب وتنبيهات النتائج', privacy: 'الخصوصية والأمان', privacySub: 'البيانات والأجهزة والحذف', help: 'مساعدة', helpSub: 'الدعم والتقييم وحل المشاكل', signOut: 'تسجيل الخروج', signOutSub: 'إنهاء هذه الجلسة على هذا الجهاز' },
    hi: { ...en, noEmail: 'ईमेल कनेक्ट नहीं है', freePlan: 'मुफ्त प्लान', proPlan: 'Pro प्लान', loading: 'प्रोफाइल मेट्रिक्स लोड हो रहे हैं...', sessions: 'सत्र', avgScore: 'औसत स्कोर', practice: 'अभ्यास', coachingFocus: 'कोचिंग फोकस', emptyCoaching: 'कोचिंग प्रोफाइल बनाने के लिए अपना पहला इंटरव्यू पूरा करें।', bestSkill: 'सर्वश्रेष्ठ कौशल', notEnoughData: 'पर्याप्त डेटा नहीं', account: 'खाता', accountSub: 'नाम, ईमेल और इंटरव्यू पहचान', edit: 'संपादित करें', subscription: 'सदस्यता', access: 'एक्सेस', active: 'सक्रिय', upgrade: 'अपग्रेड', language: 'भाषा', change: 'बदलें', notifications: 'सूचनाएं', notificationsSub: 'अभ्यास रिमाइंडर और स्कोर अलर्ट', privacy: 'गोपनीयता और सुरक्षा', privacySub: 'डेटा, डिवाइस और हटाने के नियंत्रण', help: 'मदद', helpSub: 'सहायता, स्कोरिंग और समस्या समाधान', signOut: 'साइन आउट', signOutSub: 'इस डिवाइस पर सत्र समाप्त करें' },
  };
  return copies[language] || en;
}

export default function ProfileScreen() {
  const { user, signOut } = useAuthStore();
  const { t } = useTranslation();
  const { language } = useSessionStore();
  const { cameraPresenceCoachEnabled, setCameraPresenceCoachEnabled } = useSettingsStore();
  const copy = profileCopy(language);
  const [progress, setProgress] = React.useState<ProgressData | null>(null);
  const [resumeProfile, setResumeProfile] = React.useState<ResumeProfile | null>(null);
  const [resumeBusy, setResumeBusy] = React.useState(false);
  const [resumeMessage, setResumeMessage] = React.useState('');
  const [loading, setLoading] = React.useState(true);

  const fullName = user?.user_metadata?.full_name || user?.email?.split('@')[0] || 'Candidate';
  const email = user?.email || copy.noEmail;
  const initials = fullName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part: string) => part[0]?.toUpperCase())
    .join('') || 'C';

  React.useEffect(() => {
    let mounted = true;

    const loadProfileData = async () => {
      try {
        if (user?.id) {
          const [data, resume] = await Promise.all([
            getUserProgress(user.id, language),
            getResumeProfile().catch(() => null),
          ]);
          if (mounted) {
            setProgress(data);
            setResumeProfile(resume);
          }
        }
      } catch (error) {
        console.error('[Profile] Failed to load progress:', error);
      } finally {
        if (mounted) setLoading(false);
      }
    };

    loadProfileData();
    return () => {
      mounted = false;
    };
  }, [user?.id, language]);

  const stats = progress?.stats;
  const avgScore = stats?.avg_score ?? 0;
  const sessions = stats?.sessions ?? 0;
  const practiceHours = stats?.practice_hours ?? 0;
  const hasProfileSignal = sessions > 0 && avgScore > 0;
  const bestSkill = hasProfileSignal && progress?.radar_data?.length
    ? progress.radar_data.reduce((best, item) => item.value > best.value ? item : best, progress.radar_data[0])
    : null;

  const handleSignOut = () => {
    signOut();
    router.replace('/auth/sign-in');
  };

  const handleToggleCameraCoach = async () => {
    if (cameraPresenceCoachEnabled) {
      setCameraPresenceCoachEnabled(false);
      return;
    }

    const granted = await requestPresenceCameraPermission();
    if (granted) {
      setCameraPresenceCoachEnabled(true);
    }
  };

  const handleUploadResume = async () => {
    setResumeMessage('');
    const picked = await DocumentPicker.getDocumentAsync({
      type: [
        'application/pdf',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'text/plain',
        'text/markdown',
      ],
      copyToCacheDirectory: true,
      multiple: false,
    });
    if (picked.canceled || !picked.assets?.length) {
      return;
    }

    const asset = picked.assets[0];
    try {
      setResumeBusy(true);
      const profile = await uploadResumeProfile({
        uri: asset.uri,
        name: asset.name || 'resume',
        mimeType: asset.mimeType,
      });
      setResumeProfile(profile);
      setResumeMessage('Resume saved. New interviews will use this context.');
    } catch (error: any) {
      setResumeMessage(error?.message || 'Unable to process this resume.');
    } finally {
      setResumeBusy(false);
    }
  };

  const handleDeleteResume = async () => {
    try {
      setResumeBusy(true);
      await deleteResumeProfile();
      setResumeProfile(null);
      setResumeMessage('Resume context removed.');
    } catch (error: any) {
      setResumeMessage(error?.message || 'Unable to remove resume context.');
    } finally {
      setResumeBusy(false);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView
        style={{ flex: 1 }}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.header}>
          <Text style={styles.headerTitle}>{t.profile.identity}</Text>
          <Pressable style={styles.circleBtn} onPress={() => router.push('/settings' as any)}>
            <MaterialCommunityIcons name="cog-outline" size={22} color="#FFFFFF" />
          </Pressable>
        </View>

        <View style={styles.identityCard}>
          <View style={styles.avatar}>
            <Text style={styles.avatarInitial} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.65}>
              {initials}
            </Text>
          </View>
          <View style={styles.identityInfo}>
            <Text style={styles.userName}>{fullName}</Text>
            <Text style={styles.userEmail}>{email}</Text>
            <View style={styles.planPill}>
              <Text style={styles.planText}>Beta access</Text>
            </View>
          </View>
        </View>

        {loading ? (
          <View style={styles.loadingCard}>
            <ActivityIndicator color={Colors.accent} />
            <Text style={styles.loadingText}>{copy.loading}</Text>
          </View>
        ) : (
          <View style={styles.statsRow}>
            <StatCard label={copy.sessions} value={sessions} />
            <StatCard label={copy.avgScore} value={avgScore} unit="%" />
            <StatCard label={copy.practice} value={practiceHours} unit="h" />
          </View>
        )}

        <View style={styles.summaryCard}>
          <Text style={styles.summaryLabel}>{copy.coachingFocus}</Text>
          <Text style={styles.summaryText}>
            {hasProfileSignal ? progress?.coaching_tip || copy.emptyCoaching : copy.emptyCoaching}
          </Text>
          <View style={styles.summaryMetaRow}>
            <Text style={styles.summaryMeta}>{copy.bestSkill}</Text>
            <Text style={styles.summaryMetaValue}>{bestSkill ? `${bestSkill.label} ${bestSkill.value}%` : copy.notEnoughData}</Text>
          </View>
        </View>

        <Pressable
          style={styles.cameraCoachCard}
          onPress={handleToggleCameraCoach}
          accessibilityRole="switch"
          accessibilityState={{ checked: cameraPresenceCoachEnabled }}
        >
          <View style={styles.cameraCoachIcon}>
            <Feather name="camera" size={20} color={Colors.success} />
          </View>
          <View style={styles.cameraCoachCopy}>
            <Text style={styles.cameraCoachTitle}>Camera coach</Text>
            <Text style={styles.cameraCoachSubtitle}>
              {cameraPresenceCoachEnabled
                ? 'Eye contact, head, posture, and expression feedback is enabled.'
                : 'Turn on camera presence feedback before starting an interview.'}
            </Text>
          </View>
          <View style={[styles.profileToggleTrack, !cameraPresenceCoachEnabled && styles.profileToggleTrackOff]}>
            <View style={[styles.profileToggleThumb, !cameraPresenceCoachEnabled && styles.profileToggleThumbOff]} />
          </View>
        </Pressable>

        <View style={styles.resumeCard}>
          <View style={styles.resumeHeader}>
            <View style={styles.resumeIconBox}>
              <Feather name="file-text" size={20} color={Colors.success} />
            </View>
            <View style={styles.resumeTitleWrap}>
              <Text style={styles.resumeTitle}>Resume context</Text>
              <Text style={styles.resumeSubtitle}>
                Personalizes future questions without storing the original file.
              </Text>
            </View>
          </View>

          {resumeProfile ? (
            <View style={styles.resumeDetails}>
              <Text style={styles.resumeFileName} numberOfLines={1}>{resumeProfile.file_name || 'Resume uploaded'}</Text>
              <Text style={styles.resumeSummary} numberOfLines={4}>{resumeProfile.summary}</Text>
              {resumeProfile.skills?.length ? (
                <View style={styles.skillWrap}>
                  {resumeProfile.skills.slice(0, 6).map((skill) => (
                    <View key={skill} style={styles.skillPill}>
                      <Text style={styles.skillText}>{skill}</Text>
                    </View>
                  ))}
                </View>
              ) : null}
            </View>
          ) : (
            <Text style={styles.resumeSummary}>
              Upload a PDF, DOCX, or TXT resume so Prezzence can ask questions that match your real background.
            </Text>
          )}

          {resumeMessage ? <Text style={styles.resumeMessage}>{resumeMessage}</Text> : null}

          <View style={styles.resumeActions}>
            <Pressable
              onPress={handleUploadResume}
              disabled={resumeBusy}
              style={[styles.resumeButton, resumeBusy && styles.disabledButton]}
            >
              {resumeBusy ? <ActivityIndicator color="#FFFFFF" size="small" /> : <Feather name="upload" size={16} color="#FFFFFF" />}
              <Text style={styles.resumeButtonText}>{resumeProfile ? 'Replace CV' : 'Upload CV'}</Text>
            </Pressable>
            {resumeProfile ? (
              <Pressable
                onPress={handleDeleteResume}
                disabled={resumeBusy}
                style={[styles.resumeButtonSecondary, resumeBusy && styles.disabledButton]}
              >
                <Feather name="trash-2" size={16} color={Colors.danger} />
                <Text style={styles.resumeButtonSecondaryText}>Remove</Text>
              </Pressable>
            ) : null}
          </View>
        </View>

        <View style={styles.menuSection}>
          <MenuLink
            icon="user"
            title={copy.account}
            subtitle={copy.accountSub}
            value={copy.edit}
            onPress={() => router.push('/settings/account' as any)}
          />
          <MenuLink
            icon="globe"
            title={copy.language}
            subtitle={languageLabel(language)}
            value={copy.change}
            onPress={() => router.push({ pathname: '/auth/language', params: { returnTo: 'back' } } as any)}
          />
          <MenuLink
            icon="shield"
            title={copy.privacy}
            subtitle={copy.privacySub}
            onPress={() => router.push('/profile/privacy' as any)}
          />
          <MenuLink
            icon="help-circle"
            title={copy.help}
            subtitle={copy.helpSub}
            onPress={() => router.push('/profile/help' as any)}
          />

          <View style={styles.signOutSection}>
            <MenuLink
              icon="log-out"
              title={copy.signOut}
              subtitle={copy.signOutSub}
              color={Colors.danger}
              onPress={handleSignOut}
            />
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bgPrimary,
  },
  scrollContent: {
    paddingBottom: 116,
  },
  header: {
    height: 72,
    paddingHorizontal: 20,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  headerTitle: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: '900',
  },
  circleBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  identityCard: {
    marginHorizontal: 20,
    marginTop: 8,
    marginBottom: 18,
    padding: 16,
    borderRadius: 24,
    backgroundColor: Colors.card,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  avatar: {
    width: 66,
    height: 66,
    borderRadius: 22,
    backgroundColor: 'rgba(108, 99, 255, 0.16)',
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.34)',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 2,
  },
  avatarInitial: {
    color: '#FFFFFF',
    fontSize: 23,
    fontWeight: '900',
    maxWidth: 52,
  },
  identityInfo: {
    flex: 1,
    minWidth: 0,
    marginLeft: 14,
  },
  userName: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: '900',
    marginBottom: 4,
    lineHeight: 25,
  },
  userEmail: {
    color: Colors.textSecondary,
    fontSize: 12,
    fontWeight: '600',
    marginBottom: 10,
    lineHeight: 17,
  },
  planPill: {
    alignSelf: 'flex-start',
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 5,
    backgroundColor: 'rgba(108, 99, 255, 0.14)',
  },
  planText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '900',
    textTransform: 'uppercase',
  },
  statsRow: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    gap: 12,
    marginBottom: 18,
  },
  statCard: {
    flex: 1,
    minHeight: 104,
    borderRadius: 20,
    backgroundColor: Colors.card,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    padding: 14,
    justifyContent: 'space-between',
  },
  statLabel: {
    color: Colors.textSecondary,
    fontSize: 10,
    fontWeight: '900',
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  statValueRow: {
    flexDirection: 'row',
    alignItems: 'baseline',
  },
  statValue: {
    color: '#FFFFFF',
    fontSize: 27,
    fontWeight: '900',
  },
  statUnit: {
    color: Colors.textSecondary,
    fontSize: 13,
    fontWeight: '800',
    marginLeft: 3,
  },
  loadingCard: {
    marginHorizontal: 20,
    marginBottom: 18,
    minHeight: 104,
    borderRadius: 20,
    backgroundColor: Colors.card,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
  },
  loadingText: {
    color: Colors.textSecondary,
    fontSize: 13,
    fontWeight: '700',
  },
  summaryCard: {
    marginHorizontal: 20,
    marginBottom: 18,
    padding: 18,
    borderRadius: 24,
    backgroundColor: 'rgba(108, 99, 255, 0.08)',
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.18)',
  },
  summaryLabel: {
    color: Colors.accent,
    fontSize: 11,
    fontWeight: '900',
    textTransform: 'uppercase',
    letterSpacing: 1.2,
    marginBottom: 8,
  },
  summaryText: {
    color: '#FFFFFF',
    fontSize: 14,
    lineHeight: 21,
    fontWeight: '600',
  },
  summaryMetaRow: {
    marginTop: 14,
    paddingTop: 14,
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.08)',
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
  },
  summaryMeta: {
    color: Colors.textSecondary,
    fontSize: 12,
    fontWeight: '800',
  },
  summaryMetaValue: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '900',
    flexShrink: 1,
    textAlign: 'right',
  },
  cameraCoachCard: {
    marginHorizontal: 20,
    marginBottom: 18,
    padding: 16,
    borderRadius: 24,
    backgroundColor: 'rgba(0, 214, 143, 0.08)',
    borderWidth: 1,
    borderColor: 'rgba(0, 214, 143, 0.2)',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  cameraCoachIcon: {
    width: 46,
    height: 46,
    borderRadius: 16,
    backgroundColor: 'rgba(0, 214, 143, 0.14)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  cameraCoachCopy: {
    flex: 1,
    minWidth: 0,
  },
  cameraCoachTitle: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '900',
    marginBottom: 4,
  },
  cameraCoachSubtitle: {
    color: Colors.textSecondary,
    fontSize: 12,
    lineHeight: 17,
    fontWeight: '700',
  },
  profileToggleTrack: {
    width: 48,
    height: 28,
    borderRadius: 14,
    backgroundColor: Colors.success,
    padding: 3,
    justifyContent: 'center',
  },
  profileToggleTrackOff: {
    backgroundColor: 'rgba(255, 255, 255, 0.12)',
  },
  profileToggleThumb: {
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: '#FFFFFF',
    alignSelf: 'flex-end',
  },
  profileToggleThumbOff: {
    alignSelf: 'flex-start',
    backgroundColor: 'rgba(255, 255, 255, 0.62)',
  },
  resumeCard: {
    marginHorizontal: 20,
    marginBottom: 18,
    padding: 18,
    borderRadius: 24,
    backgroundColor: 'rgba(0, 214, 143, 0.08)',
    borderWidth: 1,
    borderColor: 'rgba(0, 214, 143, 0.2)',
  },
  resumeHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 14,
  },
  resumeIconBox: {
    width: 46,
    height: 46,
    borderRadius: 16,
    backgroundColor: 'rgba(0, 214, 143, 0.14)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  resumeTitleWrap: {
    flex: 1,
    minWidth: 0,
  },
  resumeTitle: {
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '900',
  },
  resumeSubtitle: {
    color: Colors.textSecondary,
    fontSize: 12,
    lineHeight: 17,
    fontWeight: '700',
    marginTop: 3,
  },
  resumeDetails: {
    gap: 8,
  },
  resumeFileName: {
    color: Colors.success,
    fontSize: 13,
    fontWeight: '900',
  },
  resumeSummary: {
    color: '#FFFFFF',
    fontSize: 13,
    lineHeight: 20,
    fontWeight: '700',
  },
  skillWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 4,
  },
  skillPill: {
    paddingHorizontal: 10,
    minHeight: 30,
    borderRadius: 15,
    backgroundColor: 'rgba(0, 214, 143, 0.13)',
    borderWidth: 1,
    borderColor: 'rgba(0, 214, 143, 0.2)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  skillText: {
    color: Colors.success,
    fontSize: 11,
    fontWeight: '900',
  },
  resumeMessage: {
    color: Colors.warning,
    fontSize: 12,
    lineHeight: 18,
    fontWeight: '800',
    marginTop: 12,
  },
  resumeActions: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 14,
  },
  resumeButton: {
    minHeight: 44,
    borderRadius: 22,
    paddingHorizontal: 16,
    backgroundColor: Colors.success,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  resumeButtonText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '900',
  },
  resumeButtonSecondary: {
    minHeight: 44,
    borderRadius: 22,
    paddingHorizontal: 16,
    backgroundColor: 'rgba(255, 71, 87, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(255, 71, 87, 0.22)',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  resumeButtonSecondaryText: {
    color: Colors.danger,
    fontSize: 13,
    fontWeight: '900',
  },
  disabledButton: {
    opacity: 0.65,
  },
  goalCard: {
    marginHorizontal: 20,
    marginBottom: 18,
    padding: 18,
    borderRadius: 24,
    backgroundColor: Colors.card,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
  },
  goalRow: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 6,
  },
  goalInputWrap: {
    flex: 1,
  },
  goalLabel: {
    color: Colors.textSecondary,
    fontSize: 11,
    fontWeight: '900',
    textTransform: 'uppercase',
    marginBottom: 7,
  },
  goalInput: {
    minHeight: 48,
    borderRadius: 16,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    color: '#FFFFFF',
    paddingHorizontal: 12,
    fontSize: 14,
    fontWeight: '800',
  },
  goalHint: {
    color: Colors.textSecondary,
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '700',
    marginTop: 12,
  },
  goalButton: {
    alignSelf: 'flex-start',
    marginTop: 12,
    paddingHorizontal: 16,
    minHeight: 38,
    borderRadius: 19,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  goalButtonText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '900',
  },
  savedAnswerRow: {
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.08)',
    paddingTop: 12,
    marginTop: 12,
  },
  savedAnswerRole: {
    color: Colors.accent,
    fontSize: 12,
    fontWeight: '900',
    marginBottom: 6,
  },
  savedAnswerText: {
    color: '#FFFFFF',
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '700',
  },
  menuSection: {
    paddingHorizontal: 20,
    gap: 10,
  },
  menuItem: {
    minHeight: 76,
    backgroundColor: Colors.card,
    borderRadius: 20,
    padding: 14,
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.04)',
  },
  menuIconBox: {
    width: 48,
    height: 48,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 14,
  },
  menuInfo: {
    flex: 1,
    minWidth: 0,
  },
  menuTitle: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '900',
    marginBottom: 3,
  },
  menuSubtitle: {
    color: Colors.textSecondary,
    fontSize: 12,
    fontWeight: '600',
  },
  menuValue: {
    color: Colors.textSecondary,
    fontSize: 12,
    fontWeight: '900',
    marginRight: 8,
  },
  signOutSection: {
    marginTop: 16,
  },
});
