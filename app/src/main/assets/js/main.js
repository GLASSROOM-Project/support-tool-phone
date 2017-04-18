var GST = (function() {
	function executeCommand(key) {
		window.JSInterface.executeCommand(key);
	}
	
	function onVoiceInactive() {
        var speech = $('.speech');
        if (!speech) return;
        speech.removeClass('ready active error');
    }

	function onVoiceReady() {
	    var speech = $('.speech');
	    if (!speech) return;
	    speech.removeClass('active error');
	    speech.addClass('ready');
	}

	function onVoiceActive() {
	    var speech = $('.speech');
	    if (!speech) return;
	    speech.removeClass('ready error');
	    speech.addClass('active');
	}

	function onVoiceError() {
	    var speech = $('.speech');
	    if (!speech) return;
	    speech.removeClass('ready active');
	    speech.addClass('error');
	}
	
	function playMedia() {
	    var videoPlayer = document.getElementById('videoPlayer');
        videoPlayer.addEventListener("pause", function(e) {
            window.JSInterface.log("Video playback completed.");
	        window.JSInterface.mediaPlaybackComplete();
        }, false);
	    videoPlayer.play();
	}
	
	return {
		executeCommand : executeCommand,
		onVoiceInactive : onVoiceInactive,
		onVoiceReady : onVoiceReady,
		onVoiceActive : onVoiceActive,
		onVoiceError : onVoiceError,
		playMedia : playMedia
	};
})();
