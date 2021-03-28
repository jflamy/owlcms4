window.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
unlockAudioContext(window.audioCtx);

function unlockAudioContext(audioCtx) {
    console.warn("setting up unlock audio");
    if (audioCtx.state !== 'suspended') return;
    const b = document.body;
    const events = ['touchstart','touchend', 'mousedown','keydown'];
    events.forEach(e => b.addEventListener(e, unlock, false));
    function unlock() { console.warn("starting unlock"); audioCtx.resume().then(() => {clean();console.warn(" unlocked");}); }
    function clean() { events.forEach(e => b.removeEventListener(e, unlock)); }
}

function isAudioUnlocked() {
    return (window.audioCtx.state !== 'suspended');
}