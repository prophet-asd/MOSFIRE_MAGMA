package edu.ucla.astro.irlab.mosfire.mscgui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.io.File;

import javax.swing.JOptionPane;

import edu.ucla.astro.irlab.util.ColorUtilities;

/**
 * <p>Title: MSCGUIParameters</p>
 * <p>Description: Constants file for MSCGUI. Values in this class can be overriden
      by setting values in XML config file (filename passed in as argument). </p>
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Company: UCLA Infrared Laboratory</p>
 * @author Jason L. Weiss
 * @version 1.0
 */

public class MSCGUIParameters {

	//. this class follows the singleton design pattern (gamma, et al, Design Patterns)
	//. only one instance of this class is allowed.
	private static MSCGUIParameters singleton = null;

	public static File MOSFIRE_PROPERTIES_FILENAME = new File("/home/mosdev/kroot/kss/mosfire/gui/data/mscguiProperties.xml");
	public static File MOSFIRE_PARAMETERS_FILE = new File("");
	public static File LOG4J_CONFIG_FILENAME = new File("/home/mosdev/kroot/kss/mosfire/gui/data/mscgui.log4j");

	public static String MSCGUI_HELPSET_NAME = "mscguiHelp";

	//. GUI Title
	public static String GUI_TITLE = "MOSFIRE Slit Configuration GUI";
	public static String CALIBRATION_GUI_TITLE = "MOSFIRE Calibration Tool";
	public static String GUI_ACRONYM = "MSCGUI";

	//. flag for running in Engineering mode, enabling certain features. set at runtime.
	public static boolean ENGINEERING_MODE = false;

	public static boolean USE_CLASSIC_MASCGEN = false;

	//. location of upper left corner of dialog
	public static Point POINT_MAINFRAME_LOCATION = new Point(100, 100);

	//. default dialog sizes
	public static Dimension DIM_MAINFRAME = new Dimension(1250, 1020);
	public static Dimension DIM_TABLE_SLIT_LIST = new Dimension(150,150);
	public static Dimension DIM_TABLE_TARGET_LIST = new Dimension(800,210);
	public static Dimension DIM_CALIBRATION_GUI = new Dimension(350,450);
	public static Dimension DIM_CALIBRATION_PROCESS_DIALOG = new Dimension(300,450);

	public static int WIDTH_SPECTRAL_CALIBRATION_FILTER_COLUMN = 25;
	public static int WIDTH_MASCGEN_PANEL = 400;
	//. default dialog fonts
	public static Font FONT_MENU = new Font("Dialog", 0, 12);
	public static Font FONT_MENUITEM = new Font("Dialog", 0, 12);
	public static Font FONT_MASCGEN_TITLE =  new Font("Dialog", Font.BOLD, 16);
	public static Font FONT_MASK_CONFIG_LABEL = new Font("Dialog", Font.BOLD+Font.ITALIC, 16);
	public static Font FONT_MASK_CONFIG_VALUE_NAME = new Font("Dialog", Font.BOLD, 24);
	public static Font FONT_MASK_CONFIG_VALUE = new Font("Dialog", 0, 16);
	public static Font FONT_MASCGEN_LABELS = new Font("Dialog", 0, 12); 
	public static Font FONT_MASCGEN_BUTTONS = new Font("Dialog", 0, 12); 
	public static Font FONT_MASCGEN_FIELDS = new Font("Dialog", 0, 12);
	public static Font FONT_MASK_COLOR_SCALE = new Font("Dialog", 0, 12);

	public static int GUI_INSET_VERTICAL_GAP = 2;
	public static boolean SHOW_MASK_CONFIGURATION_BUTTONS = true;

	//. default colors
	public static Color COLOR_TARGET_OVERLAY = Color.YELLOW;
	public static Color COLOR_OPENED_MASKS_PANEL = new Color(205,225,255);
	public static Color COLOR_OPEN_MASK_PANEL = new Color(50,200,50);
	public static Color COLOR_LONG_SLIT_PANEL = new Color(200,200,50);
	public static Color COLOR_MASCGEN_PANEL = Color.GRAY;

	public static Color DEFAULT_COLOR_TARGET = Color.cyan;
	public static int DEFAULT_COLOR_SCALE_MODE = ColorUtilities.FULL_INTENSITY_VISIBLE_SPECTRUM;
	public static int DEFAULT_TARGET_SIZE_PERCENTAGE_OF_ROW = 20;
	
	public static int WIDTH_MASK_CONFIG_STATUS = 100;

	public static File DEFAULT_TARGET_LIST_DIRECTORY = new java.io.File("/home/mosdev/kroot/kss/mosfire/gui/mscgui/mascgen_test_data/working/");
	public static File DEFAULT_MASCGEN_PARAMS_DIRECTORY = new java.io.File("/home/mosdev/kroot/kss/mosfire/gui/mscgui/mascgen_test_data/working/");
	public static File DEFAULT_MASCGEN_OUTPUT_ROOT_DIRECTORY = new java.io.File("/home/mosdev/kroot/kss/mosfire/gui/mscgui/mascgen_test_data/working/");
	public static File DEFAULT_MASK_CONFIGURATION_ROOT_DIRECTORY = new java.io.File("/home/mosdev/kroot/kss/mosfire/gui/mscgui/mascgen_test_data/newbase/");
	public static File DEFAULT_EXECUTED_MASK_CONFIGURATION_DIRECTORY = new java.io.File("/home/mosdev/executedMasks");
	//	public static File DEFAULT_MASCGEN_OUTPUT_ROOT_DIRECTORY = new java.io.File(".");

	public static String DEFAULT_MASCGEN_PARAMS_EXTENSION = "param";

	public static int CONTEXT_MENU_MOUSE_BUTTON = 3;

	public static double TARGET_RADIUS_FACTOR = 1;
	public static double NUDGE_SLIT_AMOUNT = 0.01;

	public static int DEFAULT_MAXIMUM_SLIT_LENGTH = 15;
	public static double DEFAULT_MINIMUM_CLOSE_OFF_SLIT_WIDTH = 2.0;
	public static double DEFAULT_CLOSED_OFF_SLIT_WIDTH = 0.7;

	public static boolean REASSIGN_UNUSED_SLITS = true;

	public static String DEFAULT_MASK_NAME = "default";
	public static boolean AUTOMATIC_OUTPUT_DIR = true;
	public static boolean AUTOMATIC_OUTPUT_FORMAT = true;

	public static boolean SHOW_WARNING_DUPLICATE_MASK_NAME = true;
	public static int     DEFAULT_ANSWER_DUPLICATE_MASK_NAME_WARNING = JOptionPane.YES_OPTION;
	public static boolean SHOW_WARNING_MINIMUM_ALIGN_STARS = true;
	public static int     DEFAULT_ANSWER_MINIMUM_ALIGN_STARS = JOptionPane.YES_OPTION;
	public static boolean SHOW_WARNING_SETUP_MASK = true;
	public static int     DEFAULT_ANSWER_SETUP_MASK = JOptionPane.YES_OPTION;
	public static boolean SHOW_WARNING_EXECUTE_MASK = true;
	public static int     DEFAULT_ANSWER_EXECUTE_MASK = JOptionPane.YES_OPTION;
	public static boolean SHOW_WARNING_EXECUTE_DIFFERENT_MASK = true;
	public static int     DEFAULT_ANSWER_EXECUTE_DIFFERENT_MASK = JOptionPane.NO_OPTION;
	public static boolean SHOW_WARNING_WRITE_MSC_HTML = true;
	public static int     DEFAULT_ANSWER_WRITE_MSC_HTML = JOptionPane.YES_OPTION;
	public static boolean SHOW_WARNING_UNUSED_SLITS = true;
	public static int     DEFAULT_ANSWER_UNUSED_SLITS = JOptionPane.YES_OPTION;  

	public static boolean SCRIPT_EXECUTE_SHOW_DIALOG = false;
	public static File SCRIPT_EXECUTE_MASK = new java.io.File("/home/mosdev/kroot/kss/mosfire/scripts/control/mosfireExecuteMask");
	public static File SCRIPT_CALIBRATE_MASKS = new java.io.File("/home/mosdev/kroot/kss/mosfire/scripts/control/mosfireTakeMaskCalibrationData");

	public static int CSU_READINESS_STATES_ARRAY_OFFSET = 2;
	public static String[] CSU_READINESS_STATES = {"-2: System Stopped", 
		                                             "-1: Error", 
		                                             "0: Unknown", 
		                                             "1: System Started", 
		                                             "2: Ready for Move", 
		                                             "3: Moving", 
																				         "4: Configuring"};
	public static int CSU_READINESS_STATE_READY_TO_MOVE = 2;
	//. must be Integers instead of ints so we can use Arrays.asList().contains()
	public static Integer[] CSU_READINESS_STATES_OK_TO_SEND_TARGETS = {1,2};

	public static int DEFAULT_ITIME_SEC_ARCS = 2;
	public static int DEFAULT_ITIME_SEC_FLATS = 2;



	//. //. //. //. //. //.  SERVER NAME  //. //. //. //. //.
	public static String SERVER_NAME = "mosfire";
	public static boolean ONLINE_MODE = false;
  public static int PAUSE_MS_BETWEEN_INDIVIDUAL_SERVER_STATUS_POLLS = 1000;


	//. //. //. //. //. //. KEYWORD NAMES //. //. //. //. //.
	//. DEWAR Temperature Monitor Server Keywords


	private MSCGUIParameters() {
		//. private constructor as per singleton design pattern
	}

	public static MSCGUIParameters getInstance() {
		//. method to get instance of this singleton class

		//. if not yet defined, instantiate a new class
		if (singleton == null) {
			singleton = new MSCGUIParameters();
		}
		//. return instance
		return singleton;
	}

}
