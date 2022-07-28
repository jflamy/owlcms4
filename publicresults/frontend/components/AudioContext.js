/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
window.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
window.isIOS = iOS();
// unlockAudioContext(window.audioCtx);

// function unlockAudioContext(audioCtx) {
//     console.warn("setting up unlock audio");
//     if (audioCtx.state !== 'suspended') return;
//     const b = document.body;
//     const events = ['touchstart','touchend', 'mousedown','keydown'];
//     events.forEach(e => b.addEventListener(e, unlock, false));
//     function unlock() { console.warn("starting unlock"); audioCtx.resume().then(() => {clean();console.warn("unlocked");}); }
//     function clean() { events.forEach(e => b.removeEventListener(e, unlock)); }
// }

function iOS() {
    return [
      'iPad Simulator',
      'iPhone Simulator',
      'iPod Simulator',
      'iPad',
      'iPhone',
      'iPod'
    ].includes(navigator.platform)
    // iPad on iOS 13 detection
    || (navigator.userAgent.includes("Mac") && "ontouchend" in document)
  }
