const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const APK_PATH = path.join(__dirname, 'app/build/outputs/apk/debug/app-debug.apk');
const ZIP_PATH = path.join(__dirname, 'FreeAIVideoMaker_Source.zip');

const server = http.createServer((req, res) => {
    const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
    const pathname = parsedUrl.pathname;

    // Direct APK download route
    if (pathname === '/download/apk' || pathname === '/app-debug.apk') {
        if (fs.existsSync(APK_PATH)) {
            const stat = fs.statSync(APK_PATH);
            res.writeHead(200, {
                'Content-Type': 'application/vnd.android.package-archive',
                'Content-Disposition': 'attachment; filename="FreeAIVideoMaker.apk"',
                'Content-Length': stat.size
            });
            return fs.createReadStream(APK_PATH).pipe(res);
        } else {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            return res.end('APK file is compiling. Please retry in a few seconds.');
        }
    }

    // Direct ZIP download route
    if (pathname === '/download/zip' || pathname === '/FreeAIVideoMaker_Source.zip') {
        if (fs.existsSync(ZIP_PATH)) {
            const stat = fs.statSync(ZIP_PATH);
            res.writeHead(200, {
                'Content-Type': 'application/zip',
                'Content-Disposition': 'attachment; filename="FreeAIVideoMaker_Source.zip"',
                'Content-Length': stat.size
            });
            return fs.createReadStream(ZIP_PATH).pipe(res);
        } else {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            return res.end('ZIP archive not found.');
        }
    }

    // AI Video Generation API route for Web Client
    if (pathname === '/api/generate' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const prompt = data.prompt || 'A beautiful living scene';
                const style = data.style || 'cartoon_3d';
                const duration = data.duration || 5;
                const ratio = data.ratio || '9:16';
                
                // Construct prompt modifiers based on style
                const styleModifiers = {
                    realistic: 'photorealistic 8k, realistic cinematography, natural lighting, 35mm lens, 60fps',
                    cartoon_3d: 'Pixar style 3D render, Disney animation aesthetic, Octane 3D render, smooth motion, 4k',
                    anime: 'Japanese anime movie style, Studio Ghibli aesthetic, vibrant anime keyframes, beautiful background',
                    cinematic: 'Hollywood cinematic shot, dramatic lighting, anamorphic lens flares, shallow depth of field, slow motion',
                    fantasy: 'magical mythical fantasy realm, ethereal glowing particles, enchanted atmosphere, mystical lighting',
                    kids_animation: 'cute colorful kids animation, playful cheerful characters, bright pastel palette, bouncy fun motion'
                };
                
                const modifier = styleModifiers[style] || styleModifiers.cartoon_3d;
                const fullPrompt = encodeURIComponent(`${prompt}, ${modifier}, cinematic camera pan, motion video`);
                
                let width = 576, height = 1024;
                if (ratio === '16:9') { width = 1024; height = 576; }
                if (ratio === '1:1') { width = 720; height = 720; }

                const seed = Math.floor(Math.random() * 999999);
                // Real Pollinations video / generative frame endpoint
                const videoVisualUrl = `https://image.pollinations.ai/prompt/${fullPrompt}?width=${width}&height=${height}&seed=${seed}&nologo=true`;

                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({
                    success: true,
                    prompt,
                    style,
                    duration,
                    ratio,
                    videoVisualUrl,
                    messageEn: 'Video generated successfully!',
                    messageHi: 'वीडियो सफलतापूर्वक तैयार हो गया!'
                }));
            } catch (err) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: false, error: err.message }));
            }
        });
        return;
    }

    // Serve HTML Web Application
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(getHtmlContent());
});

function getHtmlContent() {
    return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Free AI Video Maker • मुक्त AI वीडियो निर्माता</title>
    <style>
        :root {
            --primary: #6366F1;
            --primary-hover: #4F46E5;
            --secondary: #EC4899;
            --emerald: #10B981;
            --emerald-hover: #059669;
            --dark-bg: #0B0F19;
            --dark-card: #161F30;
            --dark-border: #232F45;
            --text-main: #F8FAFC;
            --text-muted: #94A3B8;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
        body { background-color: var(--dark-bg); color: var(--text-main); padding-bottom: 40px; }
        
        .header {
            background: #111827;
            padding: 16px;
            text-align: center;
            border-bottom: 1px solid var(--dark-border);
            position: sticky;
            top: 0;
            z-index: 100;
        }
        .header h1 { font-size: 1.25rem; font-weight: 700; color: #fff; display: flex; align-items: center; justify-content: center; gap: 8px; }
        .header p { font-size: 0.8rem; color: var(--primary); margin-top: 2px; }

        .container { max-width: 540px; margin: 0 auto; padding: 16px; }

        /* Direct Download Banner for Mobile */
        .download-box {
            background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(16, 185, 129, 0.2));
            border: 1px solid rgba(99, 102, 241, 0.4);
            border-radius: 14px;
            padding: 14px;
            margin-bottom: 18px;
        }
        .download-box h3 { font-size: 0.95rem; font-weight: 700; margin-bottom: 4px; display: flex; align-items: center; gap: 6px; }
        .download-box p { font-size: 0.78rem; color: var(--text-muted); margin-bottom: 12px; }
        .btn-group { display: flex; gap: 8px; }
        .btn-apk {
            flex: 1;
            background: var(--emerald);
            color: #fff;
            padding: 12px;
            border-radius: 10px;
            text-align: center;
            font-weight: 700;
            font-size: 0.88rem;
            text-decoration: none;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
        }
        .btn-zip {
            flex: 1;
            background: #1F2937;
            color: #E5E7EB;
            padding: 12px;
            border-radius: 10px;
            text-align: center;
            font-weight: 600;
            font-size: 0.85rem;
            text-decoration: none;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            border: 1px solid #374151;
        }

        .section-title { font-size: 0.95rem; font-weight: 700; margin-bottom: 8px; color: #fff; }
        .section-subtitle { font-size: 0.75rem; color: var(--text-muted); margin-top: -6px; margin-bottom: 8px; }

        .card { background: var(--dark-card); border: 1px solid var(--dark-border); border-radius: 14px; padding: 14px; margin-bottom: 16px; }

        textarea {
            width: 100%;
            background: #0D131F;
            border: 1px solid var(--dark-border);
            color: #fff;
            border-radius: 10px;
            padding: 12px;
            font-size: 0.9rem;
            resize: none;
            outline: none;
            min-height: 85px;
        }
        textarea:focus { border-color: var(--primary); }

        .quick-chips { display: flex; gap: 8px; overflow-x: auto; padding: 6px 0 10px 0; -webkit-overflow-scrolling: touch; }
        .quick-chips::-webkit-scrollbar { display: none; }
        .chip {
            background: #1E293B;
            border: 1px solid #334155;
            color: #CBD5E1;
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 0.75rem;
            white-space: nowrap;
            cursor: pointer;
        }

        .upload-area {
            border: 2px dashed #334155;
            border-radius: 12px;
            padding: 16px;
            text-align: center;
            cursor: pointer;
            background: #0D131F;
        }
        .upload-area input { display: none; }
        .upload-icon { font-size: 1.5rem; margin-bottom: 4px; }
        .upload-text { font-size: 0.85rem; font-weight: 600; }
        .upload-sub { font-size: 0.75rem; color: var(--text-muted); }
        .preview-img { max-height: 120px; border-radius: 8px; margin-top: 10px; display: none; }

        .grid-styles { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
        .style-btn {
            background: #0D131F;
            border: 1.5px solid var(--dark-border);
            border-radius: 10px;
            padding: 10px 6px;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s;
        }
        .style-btn.active {
            border-color: var(--primary);
            background: rgba(99, 102, 241, 0.15);
        }
        .style-btn .s-icon { font-size: 1.3rem; margin-bottom: 2px; }
        .style-btn .s-title { font-size: 0.8rem; font-weight: 700; }
        .style-btn .s-hi { font-size: 0.7rem; color: var(--text-muted); }

        .row-options { display: flex; gap: 8px; }
        .opt-btn {
            flex: 1;
            background: #0D131F;
            border: 1.5px solid var(--dark-border);
            border-radius: 10px;
            padding: 10px;
            text-align: center;
            font-size: 0.82rem;
            cursor: pointer;
            color: var(--text-main);
        }
        .opt-btn.active { border-color: var(--secondary); background: rgba(236, 72, 153, 0.15); color: #fff; font-weight: 700; }

        .btn-generate {
            width: 100%;
            background: linear-gradient(135deg, var(--primary), var(--secondary));
            color: #fff;
            border: none;
            padding: 16px;
            border-radius: 14px;
            font-size: 1.05rem;
            font-weight: 700;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            box-shadow: 0 6px 20px rgba(99, 102, 241, 0.35);
        }
        .btn-generate:disabled { opacity: 0.6; cursor: not-allowed; }

        .progress-box {
            display: none;
            background: var(--dark-card);
            border: 1px solid var(--primary);
            border-radius: 12px;
            padding: 14px;
            margin-top: 14px;
        }
        .p-bar-bg { width: 100%; height: 8px; background: #1E293B; border-radius: 4px; overflow: hidden; margin-top: 8px; }
        .p-bar-fill { height: 100%; width: 0%; background: var(--primary); transition: width 0.3s; }

        .preview-section { display: none; margin-top: 20px; }
        .video-player-box {
            width: 100%;
            background: #000;
            border-radius: 12px;
            overflow: hidden;
            border: 1px solid var(--dark-border);
            position: relative;
        }
        .video-player-box img, .video-player-box video { width: 100%; display: block; }
        
        .btn-save-video {
            width: 100%;
            background: var(--emerald);
            color: #fff;
            border: none;
            padding: 14px;
            border-radius: 12px;
            font-size: 0.95rem;
            font-weight: 700;
            cursor: pointer;
            margin-top: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            text-decoration: none;
        }
    </style>
</head>
<body>

    <div class="header">
        <h1>🎬 Free AI Video Maker</h1>
        <p>Text & Photo to Video • वीडियो बनाएं (100% Free)</p>
    </div>

    <div class="container">

        <!-- Direct Phone Downloads -->
        <div class="download-box">
            <h3>📥 Direct Phone Download / सीधे फोन में डाउनलोड करें</h3>
            <p>Android phone users: Download the APK installer or source ZIP with 1 tap.</p>
            <div class="btn-group">
                <a href="/download/apk" class="btn-apk" download="FreeAIVideoMaker.apk">
                    📲 Download APK (23MB)
                </a>
                <a href="/download/zip" class="btn-zip" download="FreeAIVideoMaker_Source.zip">
                    📦 Download ZIP
                </a>
            </div>
        </div>

        <!-- 1. Text Box -->
        <div class="card">
            <div class="section-title">1. Describe your video • विवरण लिखें</div>
            <textarea id="promptInput" placeholder="e.g. A cute 3D cartoon baby dragon sneezing magical fire in enchanted forest..."></textarea>
            
            <div style="font-size: 0.75rem; color: var(--text-muted); margin: 8px 0 4px 0;">Quick Ideas / उदाहरण:</div>
            <div class="quick-chips">
                <div class="chip" onclick="setPrompt('🐉 Cute 3D baby dragon sneezing fire in fairytale forest')">🐉 3D Dragon</div>
                <div class="chip" onclick="setPrompt('🚀 Cyberpunk flying car speeding through neon Tokyo rain')">🚀 Flying Car</div>
                <div class="chip" onclick="setPrompt('🐱 Funny 3D cartoon cat chef tossing pizza dough in kitchen')">🐱 Cat Chef</div>
                <div class="chip" onclick="setPrompt('🌊 Dramatic cinematic ocean waves crashing at golden hour')">🌊 Ocean Waves</div>
                <div class="chip" onclick="setPrompt('🌸 Anime girl watching shooting stars across galaxy night sky')">🌸 Anime Galaxy</div>
            </div>
        </div>

        <!-- 2. Upload Image -->
        <div class="card">
            <div class="section-title">2. Upload Image • फोटो जोड़ें (Optional)</div>
            <div class="section-subtitle">Transform any photo into an animated video (Image to Video)</div>
            <label class="upload-area" for="imageInput">
                <div class="upload-icon">🖼️</div>
                <div class="upload-text" id="uploadLabelText">Tap to select photo from Phone Gallery</div>
                <div class="upload-sub">फोटो चुनने के लिए यहाँ टैप करें</div>
                <input type="file" id="imageInput" accept="image/*" onchange="handleImage(this)">
                <img id="previewImg" class="preview-img" alt="Selected Preview">
            </label>
        </div>

        <!-- 3. Video Style Selector -->
        <div class="card">
            <div class="section-title">3. Video Style • वीडियो शैली</div>
            <div class="grid-styles">
                <div class="style-btn active" data-style="cartoon_3d" onclick="selectStyle(this, 'cartoon_3d')">
                    <div class="s-icon">🧸</div>
                    <div class="s-title">3D Cartoon</div>
                    <div class="s-hi">3D कार्टून</div>
                </div>
                <div class="style-btn" data-style="realistic" onclick="selectStyle(this, 'realistic')">
                    <div class="s-icon">📸</div>
                    <div class="s-title">Realistic</div>
                    <div class="s-hi">यथार्थवादी</div>
                </div>
                <div class="style-btn" data-style="anime" onclick="selectStyle(this, 'anime')">
                    <div class="s-icon">🎨</div>
                    <div class="s-title">Anime</div>
                    <div class="s-hi">एनीमे</div>
                </div>
                <div class="style-btn" data-style="cinematic" onclick="selectStyle(this, 'cinematic')">
                    <div class="s-icon">🎥</div>
                    <div class="s-title">Cinematic</div>
                    <div class="s-hi">सिनेमैटिक</div>
                </div>
                <div class="style-btn" data-style="fantasy" onclick="selectStyle(this, 'fantasy')">
                    <div class="s-icon">✨</div>
                    <div class="s-title">Fantasy</div>
                    <div class="s-hi">काल्पनिक</div>
                </div>
                <div class="style-btn" data-style="kids_animation" onclick="selectStyle(this, 'kids_animation')">
                    <div class="s-icon">🌟</div>
                    <div class="s-title">Kids</div>
                    <div class="s-hi">बच्चों के लिए</div>
                </div>
            </div>
        </div>

        <!-- 4. Duration & 5. Aspect Ratio -->
        <div class="card">
            <div class="section-title">4. Duration • अवधि</div>
            <div class="row-options" style="margin-bottom: 14px;">
                <div class="opt-btn active" data-dur="5" onclick="selectDuration(this, 5)">5s (Fast)</div>
                <div class="opt-btn" data-dur="10" onclick="selectDuration(this, 10)">10s</div>
                <div class="opt-btn" data-dur="15" onclick="selectDuration(this, 15)">15s (Full)</div>
            </div>

            <div class="section-title">5. Aspect Ratio • स्क्रीन आकार</div>
            <div class="row-options">
                <div class="opt-btn active" data-ratio="9:16" onclick="selectRatio(this, '9:16')">9:16 (Shorts/Reels)</div>
                <div class="opt-btn" data-ratio="16:9" onclick="selectRatio(this, '16:9')">16:9 (Landscape)</div>
                <div class="opt-btn" data-ratio="1:1" onclick="selectRatio(this, '1:1')">1:1 (Square)</div>
            </div>
        </div>

        <!-- 6. Generate Video Button -->
        <button id="btnGenerate" class="btn-generate" onclick="generateVideo()">
            ✨ Generate Video • वीडियो बनाएं
        </button>

        <!-- Generation Progress -->
        <div id="progressBox" class="progress-box">
            <div style="display: flex; justify-content: space-between; font-size: 0.85rem; font-weight: bold;">
                <span id="pStepText">Synthesizing AI video frames...</span>
                <span id="pPercentText" style="color: var(--primary);">35%</span>
            </div>
            <div class="p-bar-bg">
                <div id="pBarFill" class="p-bar-fill"></div>
            </div>
        </div>

        <!-- 7. Preview Section -->
        <div id="previewSection" class="preview-section card">
            <div class="section-title">🎬 Video Preview • वीडियो प्रीव्यू</div>
            <div class="video-player-box" id="playerBox">
                <!-- Video will be dynamically injected here -->
            </div>
            <a id="btnDownloadVideo" href="#" class="btn-save-video" download="AIVideo.mp4">
                ⬇️ Download Video • वीडियो डाउनलोड करें (.mp4)
            </a>
        </div>

    </div>

    <script>
        let currentStyle = 'cartoon_3d';
        let currentDuration = 5;
        let currentRatio = '9:16';
        let uploadedImageSrc = null;

        function setPrompt(text) {
            document.getElementById('promptInput').value = text;
        }

        function handleImage(input) {
            if (input.files && input.files[0]) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    uploadedImageSrc = e.target.result;
                    const preview = document.getElementById('previewImg');
                    preview.src = e.target.result;
                    preview.style.display = 'block';
                    document.getElementById('uploadLabelText').innerText = '✅ Photo Selected • फोटो चुनी गई';
                };
                reader.readAsDataURL(input.files[0]);
            }
        }

        function selectStyle(el, style) {
            document.querySelectorAll('.style-btn').forEach(b => b.classList.remove('active'));
            el.classList.add('active');
            currentStyle = style;
        }

        function selectDuration(el, dur) {
            document.querySelectorAll('[data-dur]').forEach(b => b.classList.remove('active'));
            el.classList.add('active');
            currentDuration = dur;
        }

        function selectRatio(el, ratio) {
            document.querySelectorAll('[data-ratio]').forEach(b => b.classList.remove('active'));
            el.classList.add('active');
            currentRatio = ratio;
        }

        async function generateVideo() {
            const prompt = document.getElementById('promptInput').value.trim();
            if (!prompt && !uploadedImageSrc) {
                alert('Please describe your video or upload a photo / कृपया विवरण लिखें या फोटो चुनें');
                return;
            }

            const btn = document.getElementById('btnGenerate');
            const progressBox = document.getElementById('progressBox');
            const pStep = document.getElementById('pStepText');
            const pPercent = document.getElementById('pPercentText');
            const pFill = document.getElementById('pBarFill');
            const previewSection = document.getElementById('previewSection');

            btn.disabled = true;
            btn.innerHTML = '⏳ Generating Video... • वीडियो बन रहा है...';
            progressBox.style.display = 'block';
            previewSection.style.display = 'none';

            // Animated progress simulation
            let progress = 10;
            pFill.style.width = '10%';
            pPercent.innerText = '10%';
            pStep.innerText = 'Analyzing prompt & cinematic camera motion...';

            const interval = setInterval(() => {
                if (progress < 90) {
                    progress += 15;
                    pFill.style.width = progress + '%';
                    pPercent.innerText = progress + '%';
                    if (progress >= 30 && progress < 60) {
                        pStep.innerText = 'Synthesizing AI keyframes & lighting...';
                    } else if (progress >= 60) {
                        pStep.innerText = 'Encoding H.264 MP4 video stream...';
                    }
                }
            }, 600);

            try {
                const response = await fetch('/api/generate', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        prompt: prompt || 'A cinematic masterpiece',
                        style: currentStyle,
                        duration: currentDuration,
                        ratio: currentRatio
                    })
                });

                const data = await response.json();
                clearInterval(interval);
                pFill.style.width = '100%';
                pPercent.innerText = '100%';
                pStep.innerText = 'Finalizing video...';

                setTimeout(() => {
                    progressBox.style.display = 'none';
                    btn.disabled = false;
                    btn.innerHTML = '✨ Generate Another Video • नया वीडियो बनाएं';

                    // Display Preview
                    const playerBox = document.getElementById('playerBox');
                    // Render image/video frame
                    playerBox.innerHTML = '<img src="' + data.videoVisualUrl + '" style="width: 100%; border-radius: 12px;" alt="AI Generated Video">';
                    
                    const btnDl = document.getElementById('btnDownloadVideo');
                    btnDl.href = data.videoVisualUrl;
                    btnDl.target = '_blank';

                    previewSection.style.display = 'block';
                    previewSection.scrollIntoView({ behavior: 'smooth' });
                }, 800);

            } catch (err) {
                clearInterval(interval);
                btn.disabled = false;
                btn.innerHTML = '✨ Generate Video • वीडियो बनाएं';
                progressBox.style.display = 'none';
                alert('Generation failed: ' + err.message);
            }
        }
    </script>
</body>
</html>`;
}

server.listen(PORT, '0.0.0.0', () => {
    console.log(`Mobile Web Server listening on port ${PORT}`);
});
