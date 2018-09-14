package funct;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Scanner;


public class PropsManager {

	private InputStream inSt;
	private OutputStream outSt;
	private Properties props;
	private File dir;
	private File pFile;
	private StringBuilder strBld;
	private Scanner kBoard;
	
	/**
	 * Initialize with default values.
	 */
	public PropsManager() {
		kBoard = new Scanner(System.in);
		props = new Properties();
		InitProps();
		kBoard.close();
	}
	
	/**
	 * Initialize with a custom path to the file.
	 * @param path - the path where is the props file
	 */
	public PropsManager(String path) {
		props = new Properties();
		pFile = new File(path);
	}
	
	/**
	 * Initialize with a custom path to the file.
	 * @param path - the file path where is the props file
	 */
	public PropsManager(File path) {
		kBoard = new Scanner(System.in);
		props = new Properties();
		strBld = new StringBuilder(100);
		try {
			strBld.append(path.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (path.isDirectory()) {
			System.out.println("New file name:");
			strBld.append(kBoard.nextLine());
			pFile = new File(strBld.toString());
		}else if(path.exists()) {
			System.out.println("The file already exists, Do you want to overwrite? [Y/N]");
			String response;
			do {
				response = kBoard.nextLine().toUpperCase();
				switch(response) {
				case "Y":
					pFile = path;
					break;
					
				case "N":
					System.exit(0);
					break;
					
					default:
						System.out.println("Response with [Y/N]");
						break;
				}
			}while(!response.matches("^(Y|N)$"));
		}else {
			pFile = path;
		}
		SetDefaultProps();
		ExitCheck();
	}
	
	/**
	 * Gets the properties what could be saved
	 * @return the Properties which is on use.
	 */
	public Properties GetProps() {
		return props;
	}
	
	/**
	 * Sets the properties to be manipulated.
	 * @param props - a properties 
	 */
	public void SetProps(Properties props) {
		this.props = props;
	}
	
	/**
	 * Saves the properties into a .props file.
	 */
	public void SaveProps() {
		try {
			FileOutputStream fos = new FileOutputStream(pFile);
			outSt = fos;
			props.store(outSt, null);
		} catch (Exception e2) {
			e2.printStackTrace();
		} finally {
			if(outSt != null) {
				try {
					outSt.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Initializes the properties used in the class
	 * with a default file path, if doesn't exists
	 * creates the directory and file.
	 */
	private void InitProps() {
		strBld = new StringBuilder(100);
		strBld.append(System.getProperty("user.home"))
		.append(File.separator)
		.append(".Akatsuki");
		dir = new File (strBld.toString());
		
		strBld.append(File.separator)
		.append("Bot.properties");
		pFile = new File (strBld.toString());
		
		if(dir.mkdir()) {
			SetDefaultProps();
			SaveProps();
		} else {
			try {
				FileInputStream fis = new FileInputStream(pFile);
				inSt = fis;
				props.load(inSt);
			} catch (Exception e) {
				SetDefaultProps();
				SaveProps();
			} finally {
				if(inSt != null) {
					try {
						inSt.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * Loads a properties file with a specific path.
	 */
	public void LoadProps() {
		try {
			FileInputStream fis = new FileInputStream(pFile);
			inSt = fis;
			props.load(inSt);
		} catch (IOException ex) {
			ex.printStackTrace();
		}finally {
			if(inSt != null) {
				try {
					inSt.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Asks the necessary values to fill the properties with it.
	 */
	private void SetDefaultProps() {
		System.out.println("Token required: ");
		props.setProperty("token", kBoard.nextLine());
		
		System.out.println("Prefix to be used: ");
		props.setProperty("prefix", kBoard.nextLine());
		
		System.out.println("NP prefix to be used: ");
		props.setProperty("NPprefix", kBoard.nextLine());
		
		while (true) {
		System.out.println("Time zone: ");
		String tZone = kBoard.nextLine();
		if (tZone.matches("^(\\-|\\+)([0-9]|1[0-9]|2[0-3])$")) {
			tZone = "GMT" + tZone + ":00";
			props.setProperty("timeZone", tZone);
			break;
		} else {
			System.out.println("The value is not valid.");
		}
		}

		System.out.println("Select an Online Status: ");
		props.setProperty("status", kBoard.nextLine().toUpperCase());
		
		activityLoop:
		while(true) {
			System.out.println("Select an Activity Type: 1=PLAYING  2=STREAMING  3=LISTENING  4=WATCHING");
			String ActType = kBoard.nextLine();
			switch(ActType) {
			case "1":
				props.setProperty("activity", "PLAYING");
				break activityLoop;
			case "2":
				props.setProperty("activity", "STREAMING");
				break activityLoop;
			case "3":
				props.setProperty("activity", "LISTENING");
				break activityLoop;
			case "4":
				props.setProperty("activity", "WATCHING");
				break activityLoop;
			default:
				System.out.println("The value it is not valid.");
				break;
			}
		}
		
		System.out.println("Status message to be shown: ");
		props.setProperty("actMsg", kBoard.nextLine());
		
	}
	
	public void ExitCheck() {
		System.out.println("Do you want to exit? [Y/N]");
		String response;
		do {
			response = kBoard.nextLine().toUpperCase();
			switch(response) {
			case "Y":
				SaveProps();
				System.exit(0);
				break;
				
			case "N":
				SaveProps();
				break;
				
				default:
					System.out.println("Response with [Y/N]");
					break;
			}
		}while(!response.matches("^(Y|N)$"));
		kBoard.close();
	}
}
