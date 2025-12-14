import React, { useState, useEffect } from 'react';
import './TextToSpeech.css';

interface TextToSpeechProps {
  text: string;
  autoPlay?: boolean;
  onStart?: () => void;
  onEnd?: () => void;
  onPause?: () => void;
  onResume?: () => void;
}

const TextToSpeech: React.FC<TextToSpeechProps> = ({
  text,
  autoPlay = false,
  onStart,
  onEnd,
  onPause,
  onResume,
}) => {
  const [speaking, setSpeaking] = useState(false);
  const [paused, setPaused] = useState(false);
  const [voices, setVoices] = useState<SpeechSynthesisVoice[]>([]);
  const [selectedVoice, setSelectedVoice] = useState<SpeechSynthesisVoice | null>(null);
  const [rate, setRate] = useState(1.0);
  const [pitch, setPitch] = useState(1.0);

  // Check if browser supports speech synthesis
  const isSpeechSupported = typeof window !== 'undefined' && 'speechSynthesis' in window;

  // Load available voices
  useEffect(() => {
    if (!isSpeechSupported) return;

    const loadVoices = () => {
      const availableVoices = window.speechSynthesis.getVoices();
      setVoices(availableVoices);
      if (availableVoices.length > 0 && !selectedVoice) {
        // Try to find English voice, otherwise use first voice
        const englishVoice = availableVoices.find(v => v.lang.startsWith('en'));
        setSelectedVoice(englishVoice || availableVoices[0]);
      }
    };

    loadVoices();

    // Voices may load asynchronously
    if (window.speechSynthesis.onvoiceschanged !== undefined) {
      window.speechSynthesis.onvoiceschanged = loadVoices;
    }

    return () => {
      if (window.speechSynthesis.onvoiceschanged !== undefined) {
        window.speechSynthesis.onvoiceschanged = null;
      }
    };
  }, [isSpeechSupported, selectedVoice]);

  // Auto play if requested
  useEffect(() => {
    if (autoPlay && text && !speaking) {
      handleSpeak();
    }
  }, [autoPlay, text]);

  const handleSpeak = () => {
    if (!isSpeechSupported) {
      alert('Text-to-speech is not supported in this browser.');
      return;
    }

    if (!text.trim()) {
      return;
    }

    // Stop any ongoing speech
    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);

    if (selectedVoice) {
      utterance.voice = selectedVoice;
    }

    utterance.rate = rate;
    utterance.pitch = pitch;

    utterance.onstart = () => {
      setSpeaking(true);
      setPaused(false);
      onStart?.();
    };

    utterance.onend = () => {
      setSpeaking(false);
      setPaused(false);
      onEnd?.();
    };

    utterance.onerror = (error) => {
      console.error('Speech synthesis error:', error);
      setSpeaking(false);
      setPaused(false);
    };

    window.speechSynthesis.speak(utterance);
  };

  const handlePause = () => {
    if (!isSpeechSupported) return;

    window.speechSynthesis.pause();
    setPaused(true);
    onPause?.();
  };

  const handleResume = () => {
    if (!isSpeechSupported) return;

    window.speechSynthesis.resume();
    setPaused(false);
    onResume?.();
  };

  const handleStop = () => {
    if (!isSpeechSupported) return;

    window.speechSynthesis.cancel();
    setSpeaking(false);
    setPaused(false);
  };

  const handleVoiceChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const voice = voices.find(v => v.name === e.target.value);
    setSelectedVoice(voice || null);
  };

  if (!isSpeechSupported) {
    return (
      <div className="text-to-speech unsupported">
        <p>Text-to-speech is not supported in this browser.</p>
      </div>
    );
  }

  return (
    <div className="text-to-speech">
      <div className="tts-header">
        <h4>Text-to-Speech Controls</h4>
      </div>

      <div className="tts-controls">
        <div className="tts-buttons">
          {!speaking ? (
            <button
              className="btn btn-primary btn-tts"
              onClick={handleSpeak}
              title="Play"
            >
              ▶ Play
            </button>
          ) : paused ? (
            <button
              className="btn btn-success btn-tts"
              onClick={handleResume}
              title="Resume"
            >
              ▶ Resume
            </button>
          ) : (
            <button
              className="btn btn-secondary btn-tts"
              onClick={handlePause}
              title="Pause"
            >
              ⏸ Pause
            </button>
          )}

          {speaking && (
            <button
              className="btn btn-danger btn-tts"
              onClick={handleStop}
              title="Stop"
            >
              ⏹ Stop
            </button>
          )}
        </div>

        <div className="tts-settings">
          <div className="tts-setting">
            <label htmlFor="voice-select">Voice:</label>
            <select
              id="voice-select"
              value={selectedVoice?.name || ''}
              onChange={handleVoiceChange}
              disabled={speaking}
            >
              {voices.map((voice) => (
                <option key={voice.name} value={voice.name}>
                  {voice.name} ({voice.lang})
                </option>
              ))}
            </select>
          </div>

          <div className="tts-setting">
            <label htmlFor="rate-slider">
              Speed: {rate.toFixed(1)}x
            </label>
            <input
              id="rate-slider"
              type="range"
              min="0.5"
              max="2"
              step="0.1"
              value={rate}
              onChange={(e) => setRate(parseFloat(e.target.value))}
              disabled={speaking}
            />
          </div>

          <div className="tts-setting">
            <label htmlFor="pitch-slider">
              Pitch: {pitch.toFixed(1)}
            </label>
            <input
              id="pitch-slider"
              type="range"
              min="0.5"
              max="2"
              step="0.1"
              value={pitch}
              onChange={(e) => setPitch(parseFloat(e.target.value))}
              disabled={speaking}
            />
          </div>
        </div>
      </div>

      {speaking && (
        <div className="tts-status">
          <span className="status-indicator"></span>
          {paused ? 'Paused' : 'Speaking...'}
        </div>
      )}
    </div>
  );
};

export default TextToSpeech;
