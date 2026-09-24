# PulseRoute — AI Emergency Response Coordination System

PulseRoute is a premium, real-time emergency dispatch and triage management portal designed to streamline municipal emergency responses. It leverages smart routing, AI vitals diagnostics, and interactive first-aid guidance to bridge the critical gap between emergency triggers and first-responder arrivals.

---

## 🚀 Key Features

### 1. Citizen Emergency SOS Portal
- **One-Tap Emergency Alert**: Instantly alerts ambulance, fire, and police networks.
- **GPS Geolocation Lock**: Captures and broadcasts high-precision device coordinates.
- **Incident Media Upload**: Allows citizens to upload live photo/video feeds to help dispatchers analyze the situation beforehand.
- **Emergency Speed Dials**: Quick communication links for primary kin, medical advisers, and safety lines.

### 2. Live Emergency Tracker
- **Smart Routing & ETAs**: Visualizes priority corridors that avoid traffic congestions and red-light constraints.
- **Telemetry Indicators**: Tracks signal strength and lock stability.
- **Bed & ICU Slot Telemetry**: Displays live availability updates for intensive ICU beds at destination hospitals.

### 3. AI Diagnostics & Triage
- **Biometric Telemetry Analysis**: Real-time monitoring of Heart Rate, Oxygen Saturation ($SpO_2$), and Blood Pressure.
- **AI Routing Recommendation**: Recommends the optimal hospital based on patient vitals (e.g., matching a cardiac patient with a hospital containing an active cardiology wing).
- **Manual Override**: Allows operators to override recommendations and select routes manually.

### 4. Interactive First-Aid & CPR Guide
- **CPR Compression Metronome**: A pulsing visual ring paired with an audio oscillator matching the standard 100 BPM speed.
- **Narrator Voice Guide**: Reads guidelines step-by-step to assist rescuers in high-stress environments.
- **Multi-Incident Protocols**: Custom instructions for cardiac arrests, severe bleeding/trauma, and thermal burns (classified by mild and severe severities).
- **Multilingual Support**: Fully localized in English, Hindi (हिंदी), Marathi (मराठी), and Tamil (தமிழ்).

---

## 🛠️ Technology Stack

- **Frontend Core**: React 18 (loaded via unpkg CDN)
- **Compiler**: Babel Standalone (runs in-browser React transpilation)
- **Styling**: Tailwind CSS (loaded via CDN with customized color configurations)
- **Animations**: Custom Vanilla CSS keyframe animations (stored in `index.css`)
- **Audio Synthesis**: Web Audio API (Synthesized oscillators for the CPR metronome)

---

## 📦 File Structure

```bash
PulseRoute/
├── index.html       # Single-page React Application containing markup, components, and logic
├── index.css        # Custom stylesheets, keyframes, transitions, and pulse animations
└── README.md        # Project documentation
```

---

## 🚦 How to Run Locally

Since the application uses CDN dependencies and transpiles React components directly in the browser via Babel, **no compilation or build step is necessary**.

1. Clone or download this directory:
   ```bash
   cd PulseRoute
   ```
2. Launch a local development server (e.g., using VS Code Live Server, Python's http module, or `npx http-server`):
   ```bash
   # Using Python 3
   python -m http.server 8000
   
   # Or using Node.js
   npx http-server -p 8000
   ```
3. Open `http://localhost:8000` in your web browser.

---

## 🎨 Premium CSS Animations (defined in `index.css`)

- **SOS Pulse Ring (`sos-ring`)**: Staggered concentric scale-out effects representing signal broadcasts.
- **CPR Pulse (`cpr-pulse`)**: A cubic-bezier scale animation synced at exactly `0.6s` intervals (100 BPM) to instruct rescuers on timing chest compressions.
- **Map Corridor Flow (`route-line`)**: Animated dash-offset flows visually tracking dispatch path progression.
- **Smooth Page Transitions (`fade-in`)**: Modern layout reveal animations using a 3D transform slide.

---

## 🌐 Deployment Options

Since **PulseRoute** is a purely static frontend application (consisting of just `index.html` and `index.css`), it can be hosted for free on almost any static web hosting provider.

### Option A: GitHub Pages (Recommended)
1. Initialize a Git repository, commit your code, and push it to GitHub:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/yourusername/pulseroute.git
   git branch -M main
   git push -u origin main
   ```
2. On GitHub, navigate to your repository **Settings** -> **Pages**.
3. Under **Build and deployment** -> **Source**, select **Deploy from a branch**.
4. Select the `main` branch and `/root` directory, then click **Save**.
5. Your site will be live at `https://yourusername.github.io/pulseroute/` in a few minutes.

### Option B: Netlify (Instant Drag-and-Drop)
1. Go to [Netlify App](https://app.netlify.com/).
2. Drag and drop the entire `PulseRoute` folder directly into the upload area on Netlify's dashboard.
3. Your site will be deployed instantly with a custom generated URL (e.g., `https://random-name.netlify.app`).

### Option C: Vercel (Command Line)
1. Open your terminal in the project directory.
2. Run the Vercel CLI tool:
   ```bash
   npx vercel
   ```
3. Follow the interactive prompts to log in and deploy your project in seconds.
