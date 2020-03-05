package com.appleeducate.fluttermidi;

import cn.sherlock.com.sun.media.sound.SF2Soundbank;
import cn.sherlock.com.sun.media.sound.SoftSynthesizer;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import java.io.File;
import java.io.IOException;

import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.MidiDevice;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.MidiSystem;
import jp.kshoji.javax.sound.midi.Sequence;
import jp.kshoji.javax.sound.midi.Sequencer;
import jp.kshoji.javax.sound.midi.ShortMessage;
import jp.kshoji.javax.sound.midi.Transmitter;

/**
 * FlutterMidiPlugin
 */
public class FlutterMidiPlugin implements MethodCallHandler {
    private SoftSynthesizer synth;
    private Receiver recv;
    private Sequencer sequencer;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_midi");
        channel.setMethodCallHandler(new FlutterMidiPlugin());
    }

    private void prepareMidi(String path) throws IOException, MidiUnavailableException {
        System.out.println("Loading sound file into Android native " + path);
        File _file = new File(path);
        SF2Soundbank sf = new SF2Soundbank(_file);
        synth = new SoftSynthesizer();
        synth.open();
        synth.loadAllInstruments(sf);
        synth.getChannels()[0].programChange(0);
        synth.getChannels()[1].programChange(1);
        recv = synth.getReceiver();
    }

    private void sendNoteMessage(int note, int message) throws InvalidMidiDataException, MidiUnavailableException {
        if (recv != null) {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(message, 0, note, 127);
            recv.send(msg, -1);
        } else {
            throw new MidiUnavailableException("Receiver not instantiated yet");
        }
    }

    private static MidiDevice getReceivingDevice()
            throws MidiUnavailableException {
        for (MidiDevice.Info mdi : MidiSystem.getMidiDeviceInfo()) {
            MidiDevice dev = MidiSystem.getMidiDevice(mdi);
            System.out.println("Processing MIDI device " + dev);
            if (dev.getMaxReceivers() != 0) {
                String lcName = mdi.getName();
                if (lcName.contains("java")) {
                    return dev;
                }
            }
        }
        return null;
    }

    private void playCurrentMidiFile(final double tempoFactor) throws IOException, InvalidMidiDataException, MidiUnavailableException {
        float factor = (float) tempoFactor;
        System.out.println("Playing midi file with factor " + factor);
        sequencer.setTempoFactor(factor);

        new Thread(new Runnable() {
            public void run() {
                // Start playing
                sequencer.start();

                while (true) {
                    if (sequencer.isRunning()) {
                        try {
                            Thread.sleep(1000); // Check every second
                            System.out.println("Sequencer");
                        } catch (InterruptedException ignore) {
                            break;
                        }
                    } else {
                        break;
                    }
                }

                sequencer.stop();
                sequencer.close();
            }
        }).start();
    }

    private void loadMidiFile(final String path) throws IOException, InvalidMidiDataException, MidiUnavailableException {
                    String sf2 = "/data/user/0/com.example.play_music_along/app_flutter/instrument.sf2";
                    System.out.println("Loading sound file into Android native " + sf2);
                    File _file = new File(sf2);
                    SF2Soundbank sf = new SF2Soundbank(_file);
                    synth = new SoftSynthesizer();
                    synth.open();
                    synth.loadAllInstruments(sf);
                    if (synth.isOpen()) {
                        System.out.println("Synthesizer is open");
                    }
                    synth.getChannels()[0].programChange(0);
                    synth.getChannels()[1].programChange(1);
                    recv = synth.getReceiver();
                    System.out.println("Receiver " + recv);

                    sequencer = MidiSystem.getSequencer();
                    MidiSystem.addSynthesizer(synth);
                    MidiSystem.addMidiDevice(synth);
                    System.out.println("INSTRUMENTS " + sf.getInstruments()[0]);

                    System.out.println("playing midi file " + path);

//      MidiDevice receivingDevice = getReceivingDevice();
//      Transmitter tx1 = sequencer.getTransmitter();
//      Receiver rx1 = receivingDevice.getReceiver();
//      tx1.setReceiver(rx1);

                    MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
                    if (devices.length == 0) {
                        System.out.println("No MIDI devices found");
                    } else {
                        for (MidiDevice.Info dev : devices) {
                            System.out.println("Midi device: " + dev);
                        }
                    }

                    Sequence sequence = MidiSystem.getSequence(new File(path));
                    sequencer.open();
                    sequencer.setSequence(sequence);

                    if (sequencer.isOpen()) {
                        System.out.println("Sequencer is open");
                    };
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        try {
            System.out.println("Calling method " + call.method);

            switch (call.method) {
                case "prepare_midi":
                    prepareMidi((String) call.argument("path"));
                    break;

                case "change_sound":
                    prepareMidi((String) call.argument("path"));
                    break;

                case "play_midi_note":
                    sendNoteMessage((int) call.argument("note"), ShortMessage.NOTE_ON);
                    break;

                case "stop_midi_note":
                    sendNoteMessage((int) call.argument("note"), ShortMessage.NOTE_OFF);
                    break;

                case "load_midi_file":
                    loadMidiFile((String) call.argument("path"));
                    break;

                case "play_current_midi_file":
                    playCurrentMidiFile((double) call.argument("tempoFactor"));
                    break;
            }
        } catch (IOException e) {
            System.err.println("flutter_midi: Input/output exception, " + e.getMessage());
            e.printStackTrace();
        } catch (MidiUnavailableException e) {
            System.err.println("flutter_midi: Midi device is not ready, " + e.getMessage());
            e.printStackTrace();
        } catch (InvalidMidiDataException e) {
            System.err.println("flutter_midi: Invalid MID message, " + e.getMessage());
            e.printStackTrace();
        }
    }
}
