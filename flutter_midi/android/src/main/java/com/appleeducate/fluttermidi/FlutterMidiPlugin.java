package com.appleeducate.fluttermidi;

import cn.sherlock.com.sun.media.sound.SF2Soundbank;
import cn.sherlock.com.sun.media.sound.SoftSynthesizer;
import cn.sherlock.com.sun.media.sound.SoftReceiver;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.os.Handler;
import android.os.Looper;

import jp.kshoji.javax.sound.midi.ControllerEventListener;
import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.MidiDevice;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.MidiSystem;
import jp.kshoji.javax.sound.midi.Sequence;
import jp.kshoji.javax.sound.midi.Sequencer;
import jp.kshoji.javax.sound.midi.ShortMessage;
import jp.kshoji.javax.sound.midi.Transmitter;

import jp.kshoji.javax.sound.midi.MidiDevice;
import jp.kshoji.javax.sound.midi.MidiMessage;
import jp.kshoji.javax.sound.midi.ShortMessage;


/**
 * FlutterMidiPlugin
 */
public class FlutterMidiPlugin implements MethodCallHandler {
    static MethodChannel channel;
    private SoftSynthesizer synth;
    private Receiver recv;
    private Sequencer sequencer;
    private Handler uiThreadHandler = new Handler(Looper.getMainLooper());

    public class FlutterMidiReceiver extends SoftReceiver {
        public FlutterMidiReceiver(SoftSynthesizer synth) {
            super(synth);
        }

        public void send(MidiMessage message, long timeStamp) {
            if (message instanceof ShortMessage) {
                ShortMessage shortMessage = (ShortMessage) message;
                int cmd = shortMessage.getCommand();
                // System.out.println("------------------- midi message cmd " + cmd + " " + ShortMessage.NOTE_ON);
                String event = "";
                switch (cmd) {
                    case ShortMessage.NOTE_ON:
                        event = "NOTE_ON";
                        break;
                    case ShortMessage.NOTE_OFF:
                        event = "NOTE_OFF";
                        break;
                    default:
                        break;
                }
                final String eventToSend = event;
                uiThreadHandler.post(new Runnable() {
                    @Override
                    public void run () {
                        channel.invokeMethod("midiEvent", eventToSend);
                    }
                });
            }

            super.send(message, timeStamp);
        }
    }

    public class FlutterMidiSynthesizer extends SoftSynthesizer {
        @Override
        public List<Receiver> getReceivers() {
            ArrayList<Receiver> recvs = new ArrayList<Receiver>();
            recvs.addAll(super.getReceivers());
            recvs.add(new FlutterMidiReceiver(this));
            return recvs;
        }
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        channel = new MethodChannel(registrar.messenger(), "flutter_midi");
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

    private void playCurrentMidiFile(final double tempoFactor) {
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
//                sequencer.close();
            }
        }).start();
    }

    private void stopCurrentMidiFile() {
        System.out.println("Stopping midi file playing");
        sequencer.stop();
//        sequencer.close();
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

    private void loadMidiFile(final String path) throws IOException, InvalidMidiDataException, MidiUnavailableException {
        String sf2 = "/data/user/0/com.example.play_music_along/app_flutter/instrument.sf2";
        System.out.println("Loading sound file into Android native " + sf2);
        File _file = new File(sf2);
        SF2Soundbank sf = new SF2Soundbank(_file);
        synth = new FlutterMidiSynthesizer();
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

        System.out.println("Playing midi file " + path);

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

        // FIXME smoreau: set control/meta events ? eg. what message is used for tempo change ?
        int[] events = {0x58, 0x59};
        sequencer.addControllerEventListener(new ControllerEventListener() {
            @Override
            public void controlChange(ShortMessage event) {
                System.out.println("-------- PREPARING TO SEND ");
                // channel.invokeMethod("midiEvent", "NOTE_ON");
            }
        }, events);

        if (sequencer.isOpen()) {
            System.out.println("Sequencer is open");
        }
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
