/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.kaioru.distort.d4j.D4JCommandBuilder;
import co.kaioru.distort.d4j.D4JCommandRegistry;
import funct.MusicPlayer;
import funct.PropsManager;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.EmbedBuilder;

public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);
	private static IDiscordClient client;
	private static final String version = "1.3.3";
	private static int hr = Calendar.HOUR_OF_DAY, min = Calendar.MINUTE;
	private static Random rng = new Random();
	private static Properties props;
	private static PropsManager propMngr;
	private static IUser owner;

	public static void main(String[] args) {

		if (args.length == 0) {
			propMngr = new PropsManager();
			props = propMngr.GetProps();
		} else {
			switch (args[0]) {

			case "-f":
				propMngr = new PropsManager(args[1]);
				propMngr.LoadProps();
				props = propMngr.GetProps();
				break;
				
			case "-n":
				if (args.length > 1) {
					File f = new File(args[1]);
					propMngr = new PropsManager(f);
					propMngr.SaveProps();
					props = propMngr.GetProps();
				}else {
					System.out.println("-n \"path of the new file\"");
					System.exit(0);
				}
				break;

			default:
				System.out.println("The argument you are trying to use does not exist.");
				System.exit(0);
				break;
			}
		}
		
		String npPrefix = props.getProperty("NPprefix");
		String prefix = props.getProperty("prefix");
		client = new ClientBuilder().withToken(props.getProperty("token")).login();

		IListener<ReadyEvent> rdy = event -> {
			client.changePresence(StatusType.get(props.getProperty("status")), ActivityType.valueOf(props.getProperty("activity")), props.getProperty("actMsg"));
			owner = client.getApplicationOwner();
		};
		client.getDispatcher().registerListener(rdy);

		D4JCommandRegistry registry = new D4JCommandRegistry();
		registry.registerCommand(new D4JCommandBuilder("np").build(context -> {
			context.getMessage().getChannel().sendMessage(comandoNP());
			String usr = context.getMessage().getAuthor().getName();
			String guild = context.getMessage().getGuild().getName();
			String chan = context.getMessage().getChannel().getName();
			log.info(" !np request by @" + usr + " - in \"" + guild + "\" ( #" + chan + " )");
			return null;
		}));

		MusicPlayer mPlayer = new MusicPlayer();

		IListener<MessageReceivedEvent> listener = event -> {

			IMessage message = event.getMessage();
			String content = message.getContent();

			if (content.startsWith(npPrefix) && event.getAuthor() != client.getOurUser()
					&& !event.getChannel().isPrivate()) {
				try {
					String input = content.substring(1, content.length());
					registry.process(message, input);
				} catch (Exception e) {

				}

			} else if (content.startsWith(prefix) && event.getAuthor() != client.getOurUser() && content.length() > 1) {
				String cmd = content.substring(1, content.length());

				if (cmd.contains(" ")) {
					cmd = cmd.split(" ", 2)[0];
				}

				// Comandos abiertos a cualquier usuario
				global: switch (cmd) {
				case "hora":
					Calendar horaRadio = null;

					try {
						horaRadio = getDateFromNetwork();
						horaRadio.setTimeZone(TimeZone.getTimeZone(props.getProperty("timeZone")));
					} catch (IOException e) {
						System.err.println(e);
					} catch (ParseException e) {
						System.err.println(e);
					}
					message.reply("onii-chan, la hora es: **" + String.format("%02d", horaRadio.get(hr)) + ":"
							+ String.format("%02d", horaRadio.get(min)) +"**");
					break;

				// Comandos exclusivos para Superoyentes o superior
				case "playMc":
					if (event.getAuthor().getRolesForGuild(event.getGuild()).size() > 1) {
						IVoiceChannel userVoiceChannel = event.getAuthor().getVoiceStateForGuild(event.getGuild())
								.getChannel();

						if (userVoiceChannel != null) {
							mPlayer.loadAndPlay(message.getChannel(), "http://198.105.216.204/proxy/mcradio?mp=/",
									userVoiceChannel);
						} else {
							message.reply("No estás en ningún chat de voz, baka!");
						}
					} else {
						message.reply(respondeCmd(rng.nextInt(4)));
					}
					break;

				case "stop":
					if (event.getAuthor().getRolesForGuild(event.getGuild()).size() > 1) {
						IVoiceChannel botVoiceChannel = event.getClient().getOurUser()
								.getVoiceStateForGuild(event.getGuild()).getChannel();
						if (botVoiceChannel != null) {
							mPlayer.stopTrack(message.getChannel(), botVoiceChannel);
						} else {
							message.reply("No estoy en ningun chat de voz, baka!");
						}
					} else {
						message.reply(respondeCmd(rng.nextInt(4)));
					}
					break;
				// terminan comandos superoyente o superior

				// Comandos relacionados a un usuario
				case "playing":
					if (event.getAuthor().equals(owner)) {
						try {
							String game = content.split(" ", 2)[1];
							client.changePresence(client.getOurUser().getPresence().getStatus(), ActivityType.PLAYING, game);
							props.setProperty("actMsg", game);
							props.setProperty("activity", "PLAYING");
							propMngr.SetProps(props);
							propMngr.SaveProps();
							message.getChannel().sendMessage("listo ~ <:LaiLux:403437380280778752>");
						} catch (ArrayIndexOutOfBoundsException e) {
							message.getChannel().sendMessage("<:KannaQ:415745855442649089>");
						}
					} else {
						message.reply(respondeCmd(rng.nextInt(4)));
					}
					break;
					
				case "watching":
					if (event.getAuthor().equals(owner)) {
						try {
							String vid = content.split(" ", 2)[1];
							client.changePresence(client.getOurUser().getPresence().getStatus(), ActivityType.WATCHING, vid);
							props.setProperty("actMsg", vid);
							props.setProperty("activity", "WATCHING");
							propMngr.SetProps(props);
							propMngr.SaveProps();
							message.getChannel().sendMessage("listo ~ <:LaiLux:403437380280778752>");
						} catch (ArrayIndexOutOfBoundsException e) {
							message.getChannel().sendMessage("<:KannaQ:415745855442649089>");
						}
					} else {
						message.reply(respondeCmd(rng.nextInt(4)));
					}
					break;

				case "gmt":
					if (event.getAuthor().equals(owner)) {
						String tz = content.split(" ", 2)[1];
						if (tz.matches("^(\\-|\\+)([0-9]|1[0-9]|2[0-3])$")) {
							tz = "GMT" + tz + ":00";
							props.setProperty("timeZone", tz);
							propMngr.SetProps(props);
							propMngr.SaveProps();
							message.reply("ohhh... cambió la hora radio? <:KannaQ:415745855442649089>");
						} else {
							message.reply("no entiendo lo que dices... BAKA!");
						}
					} else {
						message.reply(respondeCmd(rng.nextInt(4)));
					}
					break;

				case "off":
					if (event.getAuthor().equals(owner)) {
						client.logout();
						if (!client.isLoggedIn()) {
							System.exit(0);
						}
					} else {
						message.reply(respondeCmd(rng.nextInt(4)));
					}
					break;
				// terminan comandos relacionados a un usuario

				default:
					// rol administrativo
					try {
						if (event.getAuthor().equals(owner) || event.getAuthor().hasRole(event.getGuild().getRoleByID(345352932708843523L))) {
							switch (cmd) {
							case "vr":
								message.reply("Mi versión actual es: **" + version
										+ "** ... konno baka! <:BooBlush:403437580504399872>");
								break global;
							}
						}
					} catch (NullPointerException e) {
						System.err.println(e);
						break global;
					}
					// termina rol administrativo

				} // terminan comandos abiertos
			}
			// fin comandos

		};

		client.getDispatcher().registerListener(listener);

	}

	public static EmbedObject comandoNP() {
		EmbedBuilder builder = new EmbedBuilder();

		try {
			// Se obtiene el documento de seleccion
			Document doc = Jsoup.connect("http://198.105.216.204/proxy/mcradio?mp=/").get();
			Elements tablaInfo = doc.select("table:gt(2)");
			Elements tablaDC = tablaInfo.select("td:gt(0)");

			String cancionActual = "";
			String oyentes = "";
			String tansmision = "";
			String record = "";

			// Si la tablDC esta llena procedemos a obtener lo que necesitamos
			if (!tablaDC.isEmpty()) {
				if (tablaDC.size() == 9) {
					// Para saber la transmision y los oyentes
					Element numOyentesRAW = tablaDC.get(1);

					StringTokenizer tsDatos = new StringTokenizer(numOyentesRAW.text(), " ");

					if (tsDatos.countTokens() == 11) {
						tsDatos.nextToken();
						tsDatos.nextToken();
						tsDatos.nextToken();
						tsDatos.nextToken();

						tansmision = "" + tsDatos.nextToken() + " " + tsDatos.nextToken();
						tsDatos.nextToken();

						oyentes = "" + tsDatos.nextToken();
					} else if (tsDatos.countTokens() > 11) {
						tsDatos.nextToken();
						tsDatos.nextToken();
						tsDatos.nextToken();
						tsDatos.nextToken();
						tansmision = "" + tsDatos.nextToken() + " " + tsDatos.nextToken();

						tsDatos.nextToken();

						oyentes = "" + tsDatos.nextToken().replace("(", "");

					}

					// Fin transmision de los oyentes

					// Para saber el maximo de usuarios
					Element numMaximoRAW = tablaDC.get(2);
					record = "" + numMaximoRAW.text();
					// Fin maximo usuarios

					// cancion actual
					Element nuCurrentRAW = tablaDC.get(11);
					String stCARAW = nuCurrentRAW.text();
					Integer nuInicial = stCARAW.lastIndexOf("[");
					if (!nuInicial.equals(-1)) {
						stCARAW = stCARAW.substring(0, nuInicial);
					}
					cancionActual = nuCurrentRAW.text();
					// Fin Cancion Actual

				} else {
					// Para saber la transmision y los oyentes
					Element numOyentesRAW = tablaDC.get(1);

					StringTokenizer tsDatos = new StringTokenizer(numOyentesRAW.text(), " ");

					if (tsDatos.countTokens() == 11) {
						tsDatos.nextToken();
						tsDatos.nextToken();
						tsDatos.nextToken();
						tsDatos.nextToken();

						tansmision = "" + tsDatos.nextToken() + " " + tsDatos.nextToken();
						tsDatos.nextToken();

						oyentes = "" + tsDatos.nextToken();
					} else if (tsDatos.countTokens() > 11) {
						tsDatos.nextToken();
						tsDatos.nextToken();
						tsDatos.nextToken();
						tsDatos.nextToken();
						tansmision = "" + tsDatos.nextToken() + " " + tsDatos.nextToken();

						tsDatos.nextToken();

						oyentes = "" + tsDatos.nextToken();

					}

					// Fin transmision de los oyentes

					// Para saber el maximo de usuarios
					Element numMaximoRAW = tablaDC.get(2);
					record = "" + numMaximoRAW.text();
					// Fin maximo usuarios

					// cancion actual
					Element nuCurrentRAW = tablaDC.get(11);
					String stCARAW = nuCurrentRAW.text();
					Integer nuInicial = stCARAW.lastIndexOf("[");
					if (!nuInicial.equals(-1)) {
						stCARAW = stCARAW.substring(0, nuInicial);
					}

					cancionActual = nuCurrentRAW.text();
					// Fin Cancion Actual
				}

			}

			/*
			 * stMensaje = "" + "McRadio http://198.105.216.204:8022/ \n" +
			 * "*||* **Estas escuchando:** " + " "+cancionActual+" \n" +
			 * "*||* **Oyentes:** " + ""+oyentes+" / 1000 " + "*||* **Record:** " +
			 * ""+record+" " + "*||* **Bitrate:** " + ""+tansmision+" " +
			 * "*||* **Comandos:** " + "!np";
			 */

			builder.appendField("Estas escuchando", cancionActual, false);
			builder.appendField("Oyentes", oyentes + " / 1000", true);
			builder.appendField("Record", record, true);
			builder.appendField("Bitrate", tansmision, true);
			builder.appendField("Comandos", "!np -hora -playMc -stop", true);

			builder.withColor(255, 141, 0);
			builder.withAuthorName("McRadio");
			builder.withAuthorUrl("http://mcanimeradio.com/");
			builder.withDescription("Url para reproductores http://198.105.216.204:8022/");
			builder.withFooterText("Somos como tu!");
			builder.withThumbnail("https://i.imgur.com/zI71jxb.png");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return builder.build();
	}

	public static Calendar getDateFromNetwork() throws IOException, ParseException {

		long differenceBetweenEpochs = 2208988800L;

		Socket socket = null;
		try {
			socket = new Socket("time.nist.gov", 37);
			socket.setSoTimeout(5000);

			InputStream raw = socket.getInputStream();

			long secondsSince1900 = 0;

			for (int i = 0; i < 4; i++) {
				secondsSince1900 = (secondsSince1900 << 8) | raw.read();
			}

			long secondsSince1970 = secondsSince1900 - differenceBetweenEpochs;
			long msSince1970 = secondsSince1970 * 1000;
			Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			time.setTimeInMillis(msSince1970);
			return time;
		} finally {
			try {
				if (socket != null)
					socket.close();
			} catch (IOException ex) {

			}
		}
	}

	public static String responde(int rng) {
		String rngRespuesta = "";
		switch (rng) {
		case 0:
			rngRespuesta = "";
			break;
		case 1:
			rngRespuesta = "";
			break;
		case 2:
			rngRespuesta = "";
			break;
		case 3:
			rngRespuesta = "";
			break;
		}
		return rngRespuesta;
	}

	public static String respondeCmd(int rng) {
		String rngRespuesta = "";
		switch (rng) {
		case 0:
			rngRespuesta = "no uses ese comando, konno baka!";
			break;
		case 1:
			rngRespuesta = "baaaaaka, no haré el comando <:TaigaRage:415745719606181888>";
			break;
		case 2:
			rngRespuesta = "lo siento nii-chan, no estoy <:blobpeek:415745652027424779>";
			break;
		case 3:
			rngRespuesta = "quieres ban?, no tienes permiso <:BanHamma:407065093709365248>";
			break;
		}
		return rngRespuesta;
	}
}
