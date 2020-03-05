import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'method_channel.dart';

class FlutterMidiPlatform extends PlatformInterface {
  FlutterMidiPlatform() : super(token: _token);
  static final Object _token = Object();
  static FlutterMidiPlatform _instance = MethodChannelUrlLauncher();
  static FlutterMidiPlatform get instance => _instance;
  static set instance(FlutterMidiPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  /// Needed so that the sound font is loaded
  /// On iOS make sure to include the sound_font.SF2 in the Runner folder.
  /// This does not work in the simulator.
  static Future<String> prepare(
      {@required ByteData sf2, String name = "instrument.sf2"}) async {
    print('Setup Midi..');
    throw UnimplementedError('prepare() has not been implemented.');
  }

  /// Needed so that the sound font is loaded
  /// On iOS make sure to include the sound_font.SF2 in the Runner folder.
  /// This does not work in the simulator.
  static Future<String> changeSound(
      {@required ByteData sf2, String name = "instrument.sf2"}) async {
    throw UnimplementedError('changeSound() has not been implemented.');
  }

  /// Unmute the device temporarly even if the mute switch is on or toggled in settings.
  static Future<String> unmute() async {
    throw UnimplementedError('canLaunch() has not been implemented.');
  }

  /// Use this when stopping the sound onTouchUp or to cancel a long file.
  /// Not needed if playing midi onTap.
  static Future<String> stopMidiNote({
    @required int midi,
  }) async {
    throw UnimplementedError('stopMidiNote() has not been implemented.');
  }

  /// Play a midi note from the sound_font.SF2 library bundled with the application.
  /// Play a midi note in the range between 0-256
  /// Multiple notes can be played at once as seperate calls.
  static Future<String> playMidiNote({
    @required int midi,
  }) async {
    throw UnimplementedError('playMidiNote() has not been implemented.');
  }
  
  /// Play a MIDI file.
  static Future<String> loadMidiFile({
    @required String path,
  }) async {
    throw UnimplementedError('loadMidiFile() has not been implemented.');
  }

  /// Play the current MIDI file loaded.
  static Future<String> playCurrentMidiFile({
    @required double tempoFactor,
  }) async {
    throw UnimplementedError('playCurrentMidiFile() has not been implemented.');
  }
}
