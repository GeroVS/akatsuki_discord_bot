package funct;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import exec.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.audio.IAudioManager;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.MissingPermissionsException;

import java.util.HashMap;
import java.util.Map;

public class MusicPlayer {
  private static final Logger log = LoggerFactory.getLogger(Main.class);
  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers;

  public MusicPlayer() {
    this.musicManagers = new HashMap<>();

    this.playerManager = new DefaultAudioPlayerManager();
    AudioSourceManagers.registerRemoteSources(playerManager);
    AudioSourceManagers.registerLocalSource(playerManager);
  }

  private synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild) {
    long guildId = guild.getLongID();
    GuildMusicManager musicManager = musicManagers.get(guildId);

    if (musicManager == null) {
      musicManager = new GuildMusicManager(playerManager);
      musicManagers.put(guildId, musicManager);
    }

    guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

    return musicManager;
  }

  public void loadAndPlay(final IChannel channel, final String trackUrl, IVoiceChannel vChan) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

    playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        sendMessageToChannel(channel, "Reproduciendo... nwn");

        play(channel.getGuild(), musicManager, track, vChan);
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        AudioTrack firstTrack = playlist.getSelectedTrack();

        if (firstTrack == null) {
          firstTrack = playlist.getTracks().get(0);
        }

        sendMessageToChannel(channel, "Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")");

        play(channel.getGuild(), musicManager, firstTrack, vChan);
      }

      @Override
      public void noMatches() {
        sendMessageToChannel(channel, "Nothing found by " + trackUrl);
      }

      @Override
      public void loadFailed(FriendlyException exception) {
        sendMessageToChannel(channel, "Could not play: " + exception.getMessage());
      }
    });
  }

  public void play(IGuild guild, GuildMusicManager musicManager, AudioTrack track, IVoiceChannel vChan) {
    connectVoiceChannel(guild.getAudioManager(), vChan);

    musicManager.player.playTrack(track);
  }
public void stopTrack(IChannel channel, IVoiceChannel vChan) {
	GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
	musicManager.player.stopTrack();
    sendMessageToChannel(channel, "Se detuvo la reproducci??n uwu");
    vChan.leave();
}
  
  
  public void skipTrack(IChannel channel) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
    musicManager.scheduler.nextTrack();
  }

  public void sendMessageToChannel(IChannel channel, String message) {
    try {
      channel.sendMessage(message);
    } catch (Exception e) {
      log.warn("Failed to send message {} to {}", message, channel.getName(), e);
    }
  }

  public static void connectToFirstVoiceChannel(IAudioManager audioManager) {
    for (IVoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
      if (voiceChannel.isConnected()) {
        return;
      }
    }

    for (IVoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
      try {
        voiceChannel.join();
      } catch (MissingPermissionsException e) {
        log.warn("Cannot enter voice channel {}", voiceChannel.getName(), e);
      }
    }
  }
  
  private static void connectVoiceChannel(IAudioManager audioManager, IVoiceChannel vChan) {
	    
	      if (vChan.isConnected()) {
	        return;
	      }
	    
	      try {
	        vChan.join();
	      } catch (MissingPermissionsException e) {
	        log.warn("Cannot enter voice channel {}", vChan.getName(), e);
	      }
	    }
	  }