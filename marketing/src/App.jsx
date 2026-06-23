import { useState, useEffect, useRef } from "react";
import "./App.css";

/* ─── Logo ─── */
const Logo = ({ size = 34 }) => (
  <svg viewBox="0 0 108 108" width={size} height={size} style={{ flexShrink: 0 }}>
    <rect width="108" height="108" rx="26" fill="#0D0C16" />
    <circle cx="54" cy="54" r="44" fill="#131828" />
    <circle cx="54" cy="54" r="32" fill="#1AD8A6" fillOpacity="0.88" />
    <circle cx="54" cy="54" r="20" fill="#6B5BFF" />
    <circle cx="54" cy="54" r="8" fill="#38BDF8" />
    <path d="M28,22 A40,40 0 0,1 80,22 A46,46 0 0,0 28,22" fill="#FFFFFF" fillOpacity="0.22" />
  </svg>
);

/* ─── SVG Icons ─── */
const IconMic = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="9" y="2" width="6" height="12" rx="3" /><path d="M5 10a7 7 0 0 0 14 0" /><line x1="12" y1="19" x2="12" y2="22" /><line x1="8" y1="22" x2="16" y2="22" />
  </svg>
);
const IconStar = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
  </svg>
);
const IconCamera = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M23 7 16 12l7 5V7z" /><rect x="1" y="5" width="15" height="14" rx="2" />
  </svg>
);
const IconChart = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <line x1="18" y1="20" x2="18" y2="10" /><line x1="12" y1="20" x2="12" y2="4" /><line x1="6" y1="20" x2="6" y2="14" /><line x1="2" y1="20" x2="22" y2="20" />
  </svg>
);
const IconBrain = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M9.5 2A2.5 2.5 0 0 1 12 4.5v15a2.5 2.5 0 0 1-4.96-.46 2.5 2.5 0 0 1-2.96-3.08 3 3 0 0 1-.34-5.58 2.5 2.5 0 0 1 1.32-4.24 2.5 2.5 0 0 1 1.98-3A2.5 2.5 0 0 1 9.5 2Z" />
    <path d="M14.5 2A2.5 2.5 0 0 0 12 4.5v15a2.5 2.5 0 0 0 4.96-.46 2.5 2.5 0 0 0 2.96-3.08 3 3 0 0 0 .34-5.58 2.5 2.5 0 0 0-1.32-4.24 2.5 2.5 0 0 0-1.98-3A2.5 2.5 0 0 0 14.5 2Z" />
  </svg>
);
const IconShield = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
  </svg>
);
const IconCheck = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="20 6 9 17 4 12" />
  </svg>
);
const IconMenu = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="21" y2="12" /><line x1="3" y1="18" x2="21" y2="18" />
  </svg>
);
const IconClose = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
  </svg>
);

/* ─── Persona Avatars (inline SVG, no image dependency) ─── */
const PersonaAvatar = ({ name, color1, color2, size = 88 }) => {
  const id = name.toLowerCase();
  return (
    <svg width={size} height={size} viewBox="0 0 88 88" fill="none">
      <defs>
        <linearGradient id={`pg-${id}`} x1="0" y1="0" x2="88" y2="88" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor={color1} />
          <stop offset="100%" stopColor={color2} />
        </linearGradient>
        <clipPath id={`pc-${id}`}><circle cx="44" cy="44" r="44" /></clipPath>
      </defs>
      <circle cx="44" cy="44" r="44" fill={`url(#pg-${id})`} fillOpacity="0.18" />
      <circle cx="44" cy="44" r="44" fill="none" stroke={color1} strokeWidth="1.5" strokeOpacity="0.4" />
      {/* Body */}
      <ellipse cx="44" cy="78" rx="20" ry="14" fill={color1} fillOpacity="0.35" clipPath={`url(#pc-${id})`} />
      {/* Head */}
      <circle cx="44" cy="34" r="16" fill={color1} fillOpacity="0.55" />
      <circle cx="44" cy="34" r="12" fill={color1} fillOpacity="0.7" />
      {/* Highlight */}
      <circle cx="40" cy="30" r="3.5" fill="#fff" fillOpacity="0.35" />
    </svg>
  );
};

/* ─── App Store Badges — real official images ─── */
const PlayStoreBadge = () => (
  <img src="/badge-playstore.png" alt="Get it on Google Play" className="store-badge-img" />
);

const AppStoreBadge = () => (
  <img src="/badge-appstore.svg" alt="Download on the App Store" className="store-badge-img" />
);

/* ─── Animated Phone Demo ─── */
const PhoneDemo = () => {
  const [step, setStep] = useState(0);
  const [typed, setTyped] = useState("");
  const [score, setScore] = useState(0);

  const question = "Tell me about a time you led a team through a difficult challenge.";
  const answer = "In my last role, I guided a 6-person team through a critical product launch under a tight 3-week deadline...";

  useEffect(() => {
    const sequence = [
      () => { setStep(1); },
      () => { setStep(2); },
      () => {
        setStep(3);
        let i = 0;
        const t = setInterval(() => {
          setTyped(answer.slice(0, i));
          i++;
          if (i > answer.length) clearInterval(t);
        }, 28);
      },
      () => { setStep(4); let s = 0; const t = setInterval(() => { s += 2; setScore(s); if (s >= 84) clearInterval(t); }, 20); },
      () => { setStep(5); },
    ];
    let idx = 0;
    sequence[idx]();
    const iv = setInterval(() => {
      idx++;
      if (idx < sequence.length) sequence[idx]();
      else { clearInterval(iv); setTimeout(() => { setStep(0); setTyped(""); setScore(0); }, 2000); }
    }, step === 3 ? 4000 : 1800);
    return () => clearInterval(iv);
  }, []);

  return (
    <div className="phone-wrap">
      <div className="phone-outer">
        <div className="phone-inner">
          {/* Status bar */}
          <div className="phone-status">
            <span className="phone-time">9:41</span>
            <div className="phone-dot-live"><span />LIVE</div>
          </div>

          {/* Avatar area */}
          <div className="phone-avatar-area">
            <div className={`phone-avatar-ring ${step >= 1 && step < 4 ? "speaking" : ""}`}>
              <PersonaAvatar name="maya" color1="#6C63FF" color2="#24C8F2" size={72} />
            </div>
            <div className="phone-interviewer-info">
              <span className="phone-interviewer-name">Maya</span>
              <span className="phone-interviewer-role">AI Interviewer · Job Interview</span>
            </div>
          </div>

          {/* Question */}
          {step >= 1 && (
            <div className="phone-question">
              <div className="phone-question-label">Q2 of 5</div>
              <p>{question}</p>
            </div>
          )}

          {/* Answer / state */}
          {step === 3 && (
            <div className="phone-answer">
              <div className="phone-rec"><span className="phone-rec-dot" /> Recording</div>
              <p className="phone-typed">{typed}<span className="phone-cursor" /></p>
              <div className="phone-waveform">
                {[4,7,12,8,14,6,10,13,5,9,11,7,14,8,5,12].map((h, i) => (
                  <div key={i} className="phone-bar" style={{ height: h * 2, animationDelay: `${i * 0.07}s` }} />
                ))}
              </div>
            </div>
          )}

          {step === 4 && (
            <div className="phone-scoring">
              <div className="phone-score-ring">
                <svg width="80" height="80" viewBox="0 0 80 80">
                  <circle cx="40" cy="40" r="34" fill="none" stroke="#1C1C2E" strokeWidth="6" />
                  <circle cx="40" cy="40" r="34" fill="none" stroke="url(#scoreGrad)" strokeWidth="6"
                    strokeDasharray="213.6"
                    strokeDashoffset={213.6 - (213.6 * score / 100)}
                    strokeLinecap="round"
                    transform="rotate(-90 40 40)" />
                  <defs>
                    <linearGradient id="scoreGrad" x1="0" y1="0" x2="80" y2="0">
                      <stop stopColor="#6C63FF" /><stop offset="1" stopColor="#00D68F" />
                    </linearGradient>
                  </defs>
                  <text x="40" y="45" textAnchor="middle" fill="white" fontSize="20" fontWeight="800" fontFamily="Inter,sans-serif">{score}</text>
                </svg>
              </div>
              <p className="phone-score-label">Analyzing your answer...</p>
            </div>
          )}

          {step === 5 && (
            <div className="phone-result">
              <div className="phone-result-score">84</div>
              <div className="phone-result-label">Strong Answer</div>
              <div className="phone-result-tags">
                <span className="rtag green">Clarity +12</span>
                <span className="rtag blue">Structure +9</span>
                <span className="rtag purple">Impact +11</span>
              </div>
            </div>
          )}

          {/* Bottom bar */}
          <div className="phone-bottom">
            <div className="phone-btn-row">
              <div className="phone-btn-sm">Skip</div>
              <div className="phone-btn-main">{step === 3 ? "Finish Answer" : "Answer Now"}</div>
              <div className="phone-btn-sm">Coach</div>
            </div>
          </div>
        </div>
      </div>
      {/* Floating score badge */}
      {step >= 5 && (
        <div className="phone-float-badge">
          <span>🔥</span> Session score <strong>84</strong>
        </div>
      )}
    </div>
  );
};

/* ─── Data ─── */
const features = [
  { icon: <IconBrain />, color: "#6C63FF", title: "3 AI Interviewers", desc: "Practice with lifelike 3D AI interviewers — each with a unique personality, style, and set of expectations." },
  { icon: <IconStar />, color: "#00D68F", title: "Real-time Scoring", desc: "Instant AI scores across clarity, structure, relevance, impact, and confidence after every answer." },
  { icon: <IconMic />, color: "#24C8F2", title: "Transcript Coaching", desc: "Full transcripts with word-level feedback, suggested improvements, and example model answers." },
  { icon: <IconCamera />, color: "#FF5A7A", title: "Presence Coaching", desc: "AI-powered camera analysis scores your eye contact, posture, head movement, and facial expressions live." },
  { icon: <IconChart />, color: "#FFB020", title: "Progress Tracking", desc: "Track readiness score trends, session history, and skill radar charts across every session." },
  { icon: <IconShield />, color: "#8E7DFF", title: "6 Practice Tracks", desc: "Job interviews, leadership, promotions, pitches, behavioral, and technical — tailored questions for each." },
];

const personas = [
  { name: "Maya",   role: "Peer Reviewer",    color1: "#24C8F2", color2: "#6C63FF", desc: "Friendly and collaborative. Helps you sharpen your STAR stories and delivery with supportive, focused feedback." },
  { name: "Jonas",  role: "Hiring Manager",   color1: "#6C63FF", color2: "#8E7DFF", desc: "Direct, professional, and results-focused. Asks tough questions and expects clear evidence of impact." },
  { name: "Sophia", role: "Domain Expert",    color1: "#FF5A7A", color2: "#FFB020", desc: "Deep domain knowledge across tech, finance, and leadership. Tests breadth and depth with probing follow-ups." },
];

const tracks = [
  { name: "Job Interview",  desc: "Tailor questions to any role, company, or industry.",    color: "#6C63FF" },
  { name: "Promotion",      desc: "Demonstrate readiness, impact, and leadership potential.", color: "#00D68F" },
  { name: "Pitch",          desc: "Sharpen investor, sales, or product presentation answers.", color: "#FFB020" },
  { name: "Leadership",     desc: "Practice executive presence and people leadership decisions.", color: "#24C8F2" },
  { name: "Behavioral",     desc: "Build powerful STAR stories and situational examples.",    color: "#FF5A7A" },
  { name: "Technical",      desc: "Answer specialist, role-specific, and system-design questions.", color: "#8E7DFF" },
];

const pricingFree = [
  "3 free sessions per month",
  "Sophia AI interviewer",
  "Basic scoring & transcript",
  "Session history",
];

const pricingPro = [
  "Unlimited interview sessions",
  "All 3 AI interviewers",
  "Deep coaching + model answers",
  "Camera presence coaching",
  "Full radar chart reports",
  "PDF export & share",
  "Company research mode",
  "Panel interview mode",
];

/* ─── Main App ─── */
export default function App() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    window.addEventListener("scroll", onScroll);
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const navLinks = [
    { href: "#features", label: "Features" },
    { href: "#interviewers", label: "Interviewers" },
    { href: "#tracks", label: "Tracks" },
    { href: "#pricing", label: "Pricing" },
  ];

  return (
    <>
      {/* ─── Nav ─── */}
      <nav className={`nav ${scrolled ? "nav-scrolled" : ""}`}>
        <a href="/" className="nav-logo">
          <Logo size={34} />
          Prezzence
        </a>
        <ul className="nav-links">
          {navLinks.map(l => <li key={l.href}><a href={l.href}>{l.label}</a></li>)}
        </ul>
        <a href="https://play.google.com/store/apps/details?id=com.pollecode.prezzence" className="nav-cta">
          Download Free
        </a>
        <button className="nav-hamburger" onClick={() => setMenuOpen(v => !v)} aria-label="Menu">
          {menuOpen ? <IconClose /> : <IconMenu />}
        </button>
      </nav>

      {/* Mobile Menu */}
      <div className={`mobile-menu ${menuOpen ? "open" : ""}`}>
        {navLinks.map(l => (
          <a key={l.href} href={l.href} className="mobile-link" onClick={() => setMenuOpen(false)}>{l.label}</a>
        ))}
        <a href="https://play.google.com/store/apps/details?id=com.pollecode.prezzence" className="mobile-cta">
          Download Free on Google Play
        </a>
      </div>

      {/* ─── Hero ─── */}
      <section className="hero">
        <div className="hero-bg">
          <div className="orb orb-1" />
          <div className="orb orb-2" />
          <div className="orb orb-3" />
          <div className="hero-grid" />
        </div>

        <div className="hero-content">
          <div className="hero-text">
            <div className="hero-badge">
              <span className="badge-dot" />
              AI-Powered · Live 3D Interviewers
            </div>

            <h1>
              Practice interviews.<br />
              <span className="h1-gradient">Land the job.</span>
            </h1>

            <p className="hero-sub">
              Train with lifelike AI interviewers that ask real questions, score your answers instantly, and coach you to improve with every session.
            </p>

            <div className="hero-badges-row">
              <a href="https://play.google.com/store/apps/details?id=com.pollecode.prezzence" className="store-btn">
                <PlayStoreBadge />
              </a>
              <a href="https://apps.apple.com/app/prezzence" className="store-btn">
                <AppStoreBadge />
              </a>
            </div>

            <div className="hero-stats">
              <div className="stat"><span className="stat-val stat-accent">3</span><span className="stat-lbl">AI Interviewers</span></div>
              <div className="stat-divider" />
              <div className="stat"><span className="stat-val stat-accent">6</span><span className="stat-lbl">Practice Tracks</span></div>
              <div className="stat-divider" />
              <div className="stat"><span className="stat-val stat-accent">Live</span><span className="stat-lbl">AI Feedback</span></div>
            </div>
          </div>

          <div className="hero-visual">
            <PhoneDemo />
          </div>
        </div>
      </section>

      {/* ─── Features ─── */}
      <section className="section" id="features">
        <div className="section-header">
          <div className="section-pill">Features</div>
          <h2>Everything you need to interview with confidence</h2>
          <p>Prezzence combines realistic AI interviewers with deep analytics so every session makes you measurably better.</p>
        </div>
        <div className="features-grid">
          {features.map((f) => (
            <div className="feature-card" key={f.title} style={{ "--card-color": f.color }}>
              <div className="feature-icon-wrap" style={{ background: f.color + "20" }}>
                <span style={{ color: f.color }}>{f.icon}</span>
              </div>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ─── Interviewers ─── */}
      <section className="section section-dark" id="interviewers">
        <div className="section-header">
          <div className="section-pill">AI Interviewers</div>
          <h2>Choose your interviewer</h2>
          <p>Three distinct AI personalities give you the full spectrum of interview styles, pressure levels, and feedback approaches.</p>
        </div>
        <div className="personas-grid">
          {personas.map((p) => (
            <div className="persona-card" key={p.name} style={{ "--pcolor": p.color1 }}>
              <div className="persona-avatar-wrap">
                <PersonaAvatar name={p.name} color1={p.color1} color2={p.color2} size={80} />
              </div>
              <div className="persona-name">{p.name}</div>
              <div className="persona-role" style={{ color: p.color1 }}>{p.role}</div>
              <p className="persona-desc">{p.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ─── How It Works ─── */}
      <section className="section" id="how">
        <div className="section-header">
          <div className="section-pill">How it works</div>
          <h2>From nervous to confident in four steps</h2>
          <p>Start a session in under a minute and leave with actionable feedback every time.</p>
        </div>
        <div className="steps-row">
          {[
            { n: "01", title: "Pick your track", desc: "Choose from job interviews, leadership, behavioral, technical, or pitch sessions tailored to your goal." },
            { n: "02", title: "Meet your interviewer", desc: "Select an AI interviewer whose style matches the pressure you want — supportive, tough, or panel-style." },
            { n: "03", title: "Answer out loud", desc: "Speak your answers naturally. The AI listens, transcribes, and asks follow-up questions in real time." },
            { n: "04", title: "Get instant coaching", desc: "Review your score breakdown, read the transcript with suggestions, and see a model answer for every question." },
          ].map((s, i) => (
            <div className="step-card" key={s.n}>
              <div className="step-num">{s.n}</div>
              {i < 3 && <div className="step-connector" />}
              <h3>{s.title}</h3>
              <p>{s.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ─── Tracks ─── */}
      <section className="section section-dark" id="tracks">
        <div className="section-header">
          <div className="section-pill">Practice Tracks</div>
          <h2>Match any interview you'll face</h2>
          <p>Every track uses questions tailored to its format — so your practice is always relevant.</p>
        </div>
        <div className="tracks-grid">
          {tracks.map((t) => (
            <div className="track-card" key={t.name} style={{ "--tcolor": t.color }}>
              <div className="track-bar" style={{ background: t.color }} />
              <div className="track-body">
                <h4>{t.name}</h4>
                <p>{t.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ─── Pricing ─── */}
      <section className="section" id="pricing">
        <div className="section-header">
          <div className="section-pill">Pricing</div>
          <h2>Start free. Go Pro when you're ready.</h2>
          <p>No credit card required. Upgrade any time before an important interview.</p>
        </div>
        <div className="pricing-grid">
          {/* Free */}
          <div className="pricing-card">
            <div className="pricing-tier">Free</div>
            <div className="pricing-price"><span className="price-amt">$0</span><span className="price-period"> forever</span></div>
            <p className="pricing-desc">Everything you need to start building confidence.</p>
            <ul className="pricing-list">
              {pricingFree.map(f => (
                <li key={f}><span className="check-icon check-free"><IconCheck /></span>{f}</li>
              ))}
            </ul>
            <a href="https://play.google.com/store/apps/details?id=com.pollecode.prezzence" className="pricing-btn pricing-btn-free">
              Download Free
            </a>
          </div>

          {/* Pro */}
          <div className="pricing-card pricing-card-pro">
            <div className="pricing-card-glow" />
            <div className="pricing-pro-badge">Most Popular</div>
            <div className="pricing-tier">Pro</div>
            <div className="pricing-price"><span className="price-amt">$9.99</span><span className="price-period"> / month</span></div>
            <p className="pricing-desc">For serious candidates who want every advantage.</p>
            <ul className="pricing-list">
              {pricingPro.map(f => (
                <li key={f}><span className="check-icon check-pro"><IconCheck /></span>{f}</li>
              ))}
            </ul>
            <a href="https://play.google.com/store/apps/details?id=com.pollecode.prezzence" className="pricing-btn pricing-btn-pro">
              Start Pro Today
            </a>
          </div>
        </div>
      </section>

      {/* ─── CTA ─── */}
      <section className="cta-section">
        <div className="cta-orb cta-orb-1" />
        <div className="cta-orb cta-orb-2" />
        <div className="cta-content">
          <h2>Your next interview is closer than you think.</h2>
          <p>Download Prezzence and do your first practice session today — it's free.</p>
          <div className="cta-badges">
            <a href="https://play.google.com/store/apps/details?id=com.pollecode.prezzence" className="store-btn">
              <PlayStoreBadge />
            </a>
            <a href="https://apps.apple.com/app/prezzence" className="store-btn">
              <AppStoreBadge />
            </a>
          </div>
        </div>
      </section>

      {/* ─── Footer ─── */}
      <footer className="footer">
        <div className="footer-inner">
          <div className="footer-top">
            <a href="/" className="nav-logo footer-logo">
              <Logo size={28} />
              Prezzence
            </a>
            <nav className="footer-links">
              {navLinks.map(l => <a key={l.href} href={l.href}>{l.label}</a>)}
              <a href="/privacy">Privacy</a>
              <a href="/terms">Terms</a>
            </nav>
          </div>
          <div className="footer-bottom">
            <p>AI avatars powered by <strong>DUIX</strong></p>
            <p>&copy; {new Date().getFullYear()} Prezzence. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </>
  );
}
