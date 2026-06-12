import "./App.css";

const features = [
  {
    icon: "\u{1F916}",
    color: "#6C63FF",
    title: "AI Interviewers",
    desc: "Practice with lifelike 3D AI interviewers who ask realistic questions and respond to your answers in real time.",
  },
  {
    icon: "\u{1F3AF}",
    color: "#00D68F",
    title: "Smart Scoring",
    desc: "Get instant AI feedback on your answers with detailed scoring across clarity, relevance, structure, and confidence.",
  },
  {
    icon: "\u{1F4AC}",
    color: "#24C8F2",
    title: "Transcript Analysis",
    desc: "Review full transcripts of your interviews with highlighted areas for improvement and suggested better answers.",
  },
  {
    icon: "\u{1F4F7}",
    color: "#FF5A7A",
    title: "Presence Coaching",
    desc: "Receive feedback on your speaking pace, filler words, eye contact, and body language through camera analysis.",
  },
  {
    icon: "\u{1F4CA}",
    color: "#FFB020",
    title: "Progress Tracking",
    desc: "Track your improvement over time with detailed session history, score trends, and personalized coaching insights.",
  },
  {
    icon: "\u{1F465}",
    color: "#8E7DFF",
    title: "Multiple Tracks",
    desc: "Choose from job interviews, promotions, pitches, leadership, behavioral, and technical practice tracks.",
  },
];

const tracks = [
  { name: "Job Interview", desc: "Practice for a role, company, or hiring panel.", color: "#6C63FF" },
  { name: "Promotion", desc: "Prepare to show impact, readiness, and leadership.", color: "#00D68F" },
  { name: "Pitch", desc: "Sharpen investor, sales, or presentation answers.", color: "#FFB020" },
  { name: "Leadership", desc: "Practice executive presence and people decisions.", color: "#24C8F2" },
  { name: "Behavioral", desc: "Build stronger STAR stories and examples.", color: "#FF5A7A" },
  { name: "Technical", desc: "Answer specialist and role-specific questions.", color: "#8E7DFF" },
];

const personas = [
  {
    name: "Sofia",
    title: "Executive Coach",
    img: "/personas/sofia.jpg",
    desc: "A warm and experienced coach who challenges you with thoughtful follow-up questions.",
  },
  {
    name: "Oliver",
    title: "Hiring Manager",
    img: "/personas/oliver.jpg",
    desc: "A direct and professional interviewer focused on your qualifications and fit.",
  },
  {
    name: "Lily",
    title: "Peer Reviewer",
    img: "/personas/lily.jpg",
    desc: "A friendly collaborator who helps you refine your stories and delivery.",
  },
];

export default function App() {
  return (
    <>
      <nav className="nav">
        <a href="/" className="nav-logo">
          <div className="nav-logo-icon">P</div>
          Prezzence
        </a>
        <ul className="nav-links">
          <li><a href="#features">Features</a></li>
          <li><a href="#personas">Interviewers</a></li>
          <li><a href="#tracks">Tracks</a></li>
          <li><a href="#how">How It Works</a></li>
          <li><a href="https://prezzence.app/download" className="nav-cta">Get Started</a></li>
        </ul>
      </nav>

      <section className="hero">
        <div className="hero-bg">
          <div className="hero-gradient hero-gradient-1" />
          <div className="hero-gradient hero-gradient-2" />
          <div className="hero-grid" />
        </div>
        <div className="hero-content">
          <div className="hero-text">
            <div className="hero-badge">
              <span className="hero-badge-dot" />
              Now available on Android
            </div>
            <h1>
              Master your next<br />
              <span className="highlight">interview with AI</span>
            </h1>
            <p>
              Practice interviews with lifelike 3D AI interviewers. Get instant
              feedback, track your progress, and build confidence before the real thing.
            </p>
            <div className="hero-buttons">
              <a href="https://prezzence.app/download" className="btn-primary">
                Download Now &rarr;
              </a>
              <a href="#features" className="btn-secondary">
                Learn More
              </a>
            </div>
            <div className="hero-stats">
              <div>
                <div className="hero-stat-value">6</div>
                <div className="hero-stat-label">Interview Tracks</div>
              </div>
              <div>
                <div className="hero-stat-value">3</div>
                <div className="hero-stat-label">AI Interviewers</div>
              </div>
              <div>
                <div className="hero-stat-value">Real-time</div>
                <div className="hero-stat-label">AI Feedback</div>
              </div>
            </div>
          </div>
          <div className="hero-image">
            <img src="/screens/home.png" alt="Prezzence app screenshot" />
          </div>
        </div>
      </section>

      <section className="section" id="features">
        <div className="section-header">
          <h2>Everything you need to succeed</h2>
          <p>
            Prezzence combines realistic AI interviewers with smart analytics
            to help you prepare with confidence.
          </p>
        </div>
        <div className="features-grid">
          {features.map((f) => (
            <div className="feature-card" key={f.title}>
              <div className="feature-icon" style={{ background: `${f.color}1A` }}>
                {f.icon}
              </div>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="section" id="personas">
        <div className="section-header">
          <h2>Choose your AI interviewer</h2>
          <p>
            Each interviewer has a unique personality and style to give you
            diverse practice experience.
          </p>
        </div>
        <div className="personas-grid">
          {personas.map((p) => (
            <div className="persona-card" key={p.name}>
              <img className="persona-avatar" src={p.img} alt={p.name} />
              <h3>{p.name}</h3>
              <div className="persona-title">{p.title}</div>
              <p>{p.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="section" id="tracks">
        <div className="section-header">
          <h2>Practice any interview type</h2>
          <p>
            Tailored practice tracks designed to match real interview formats
            across different scenarios.
          </p>
        </div>
        <div className="tracks-grid">
          {tracks.map((t) => (
            <div className="track-card" key={t.name}>
              <div className="track-dot" style={{ background: t.color }} />
              <div className="track-info">
                <h4>{t.name}</h4>
                <p>{t.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="section" id="how">
        <div className="section-header">
          <h2>How it works</h2>
          <p>Get started in minutes and see improvement with every session.</p>
        </div>
        <div className="steps">
          <div className="step">
            <div className="step-number">1</div>
            <h3>Choose a track</h3>
            <p>Pick from six interview tracks tailored to your goals.</p>
          </div>
          <div className="step">
            <div className="step-number">2</div>
            <h3>Start practicing</h3>
            <p>Answer questions from your chosen AI interviewer naturally.</p>
          </div>
          <div className="step">
            <div className="step-number">3</div>
            <h3>Get feedback</h3>
            <p>Review AI-powered scoring and transcript analysis instantly.</p>
          </div>
          <div className="step">
            <div className="step-number">4</div>
            <h3>Track progress</h3>
            <p>Monitor your improvement with detailed session history and trends.</p>
          </div>
        </div>
      </section>

      <section className="cta-section">
        <div className="cta-gradient" />
        <div className="cta-content">
          <h2>Ready to ace your next interview?</h2>
          <p>
            Join thousands of professionals who prepare with Prezzence.
          </p>
          <div className="cta-buttons">
            <a href="https://prezzence.app/download" className="btn-primary">
              Download Now &rarr;
            </a>
            <a href="#features" className="btn-secondary">
              Explore Features
            </a>
          </div>
        </div>
      </section>

      <footer className="footer">
        <p>&copy; {new Date().getFullYear()} Prezzence. All rights reserved.</p>
      </footer>
    </>
  );
}
