/* Copyright (c) 2012, Regents of the University of California
 * All rights reserved.
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for educational, research and non-profit purposes, without 
 * fee, and without a written agreement is hereby granted, provided that the 
 * above copyright notice, this paragraph and the following three paragraphs 
 * appear in all copies.
 * 
 * Permission to incorporate this software into commercial products may be 
 * obtained by contacting the University of California.
 * 
 *  Thomas J. Trappler, ASM
 *  Director, UCLA Software Licensing
 *  UCLA Office of Information Technology
 *  5611 Math Sciences
 *  Los Angeles, CA 90095-1557
 *  (310) 825-7516
 *  trappler@ats.ucla.edu
 *  
 *  This software program and documentation are copyrighted by The Regents of 
 *  the University of California. The software program and documentation are 
 *  supplied "as is", without any accompanying services from The Regents. The 
 *  Regents does not warrant that the operation of the program will be 
 *  uninterrupted or error-free. The end-user understands that the program was 
 *  developed for research purposes and is advised not to rely exclusively on 
 *  the program for any reason.
 *  
 *  IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR 
 *  DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING 
 *  LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS 
 *  DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE 
 *  POSSIBILITY OF SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY 
 *  DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE 
 *  SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF 
 *  CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, 
 *  ENHANCEMENTS, OR MODIFICATIONS.
 */

package edu.ucla.astro.irlab.mosfire.util;

import static edu.ucla.astro.irlab.mosfire.util.MosfireParameters.CSU_SLIT_TILT_ANGLE;
import static edu.ucla.astro.irlab.mosfire.util.MosfireParameters.CSU_SLIT_TILT_ANGLE_RADIANS;
import static edu.ucla.astro.irlab.mosfire.util.MosfireParameters.CSU_FP_RADIUS;
import static edu.ucla.astro.irlab.mosfire.util.MosfireParameters.CSU_HEIGHT;
import static edu.ucla.astro.irlab.mosfire.util.MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS;
import static edu.ucla.astro.irlab.mosfire.util.MosfireParameters.CSU_WIDTH;
import static edu.ucla.astro.irlab.mosfire.util.MosfireParameters.OVERLAP;
import static edu.ucla.astro.irlab.mosfire.util.MosfireParameters.SINGLE_SLIT_HEIGHT;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import nom.tam.fits.AsciiTable;
import nom.tam.fits.AsciiTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.BufferedFile;

import org.apache.log4j.Logger;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import edu.ucla.astro.irlab.util.CombinationGenerator;
import edu.ucla.astro.irlab.util.NumberFormatters;

//. TODO history (written on mascgen, and on modifies)
/**
 * This class contains all information about a MOSFIRE Slit Mask configuration for the
 * cryogenic slit unit (CSU).  A full description includes the following:
 * <ul>
 *   <li> Mechanical Slit List giving list of bar positions in CSU space and target in each slit.  There is a mechanical slit for every row in the CSU (46).
 *   <li> Science Slit List giving a list of contiguous slits in mask, and target in each slit.  There is a science slit for evey contiguous slit in mask ( =< 46).
 *   <li> Alignment Slit List giving list of alignment stars for mask.  Contains both science slit and mechanical slit information.
 *   <li> Mask Name
 *   <li> MASCGEN Result specifying pointing and position angle for mask, as well as objects and alignment stars in mask.
 *   <li> MASCGEN Arguments used to create mask. 
 * </ul>
 * 
 * This class also contains methods to write the associated data products that describes the mask.  This includes:
 * 
 * <ul>
 *   <li>MASCGEN parameters file
 *   <li>Target list for targets in mask
 *   <li>Target list for all original targets
 *   <li>MOSFIRE Slit Configuration (MSC) file
 *   <li>Description of slits
 *   <li>Script to execute science mask
 *   <li>Script to execute alignment mask (if there are alignment stars in configuration)
 *   <li>DS9 regions file
 *   <li>Keck formatted star list for mask pointing
 *   <li>(Optional) HTML version of configuration
 * </ul>
 * 
 * A slit configuration can be constructed from a <code>MascgenResult</code>, or can be an automatically generated
 * configuration for an open mask, or a long slit configuration.
 * 
 * @author Jason L. Weiss, UCLA Infrared Laboratory
 * @see MascgenResult
 *
 */
public class SlitConfigurationNEW implements Cloneable {
	private static final Logger logger = Logger.getLogger(SlitConfigurationNEW.class);

	public static final String XML_ROOT = "slitConfiguration";
	public static final String XML_ATTRIBUTE_MSC_VERSION="mscVersion";
	public static final String XML_ELEMENT_MASK_DESCRIPTION = "maskDescription";
	public static final String XML_ATTRIBUTE_MASK_NAME = "maskName";
	public static final String XML_ATTRIBUTE_TOTAL_PRIORITY = "totalPriority";
	public static final String XML_ATTRIBUTE_CENTER_RAH = "centerRaH";
	public static final String XML_ATTRIBUTE_CENTER_RAM = "centerRaM";
	public static final String XML_ATTRIBUTE_CENTER_RAS = "centerRaS";
	public static final String XML_ATTRIBUTE_CENTER_DECD = "centerDecD";
	public static final String XML_ATTRIBUTE_CENTER_DECM = "centerDecM";
	public static final String XML_ATTRIBUTE_CENTER_DECS = "centerDecS";
	public static final String XML_ATTRIBUTE_MASK_PA = "maskPA";
	public static final String XML_ELEMENT_MECHANICAL_SLIT_CONFIG = "mechanicalSlitConfig";
	public static final String XML_ELEMENT_MECHANICAL_SLIT = "mechanicalSlit";
	public static final String XML_ATTRIBUTE_SLIT_NUMBER = "slitNumber";
	public static final String XML_ATTRIBUTE_LEFT_BAR_NUMBER = "leftBarNumber";
	public static final String XML_ATTRIBUTE_RIGHT_BAR_NUMBER = "rightBarNumber";
	public static final String XML_ATTRIBUTE_LEFT_BAR_POSITION_MM = "leftBarPositionMM";
	public static final String XML_ATTRIBUTE_RIGHT_BAR_POSITION_MM = "rightBarPositionMM";
	public static final String XML_ATTRIBUTE_CENTER_POSITION = "centerPositionArcsec";
	public static final String XML_ATTRIBUTE_SLIT_WIDTH = "slitWidthArcsec";
	public static final String XML_ATTRIBUTE_TARGET = "target";
	public static final String XML_ELEMENT_SCIENCE_SLIT_CONFIG = "scienceSlitConfig";
	public static final String XML_ELEMENT_SCIENCE_SLIT = "scienceSlit";
	public static final String XML_ATTRIBUTE_SLIT_RAH = "slitRaH";
	public static final String XML_ATTRIBUTE_SLIT_RAM = "slitRaM";
	public static final String XML_ATTRIBUTE_SLIT_RAS = "slitRaS";
	public static final String XML_ATTRIBUTE_SLIT_DECD = "slitDecD";
	public static final String XML_ATTRIBUTE_SLIT_DECM = "slitDecM";
	public static final String XML_ATTRIBUTE_SLIT_DECS = "slitDecS";
	public static final String XML_ATTRIBUTE_SLIT_LENGTH = "slitLengthArcsec";
	public static final String XML_ATTRIBUTE_TARGET_PRIORITY = "targetPriority";
	public static final String XML_ATTRIBUTE_TARGET_MAGNITUDE = "targetMag";
	public static final String XML_ATTRIBUTE_TARGET_CENTER_DISTANCE = "targetCenterDistance";
	public static final String XML_ATTRIBUTE_TARGET_RAH = "targetRaH";
	public static final String XML_ATTRIBUTE_TARGET_RAM = "targetRaM";
	public static final String XML_ATTRIBUTE_TARGET_RAS = "targetRaS";
	public static final String XML_ATTRIBUTE_TARGET_DECD = "targetDecD";
	public static final String XML_ATTRIBUTE_TARGET_DECM = "targetDecM";
	public static final String XML_ATTRIBUTE_TARGET_DECS = "targetDecS";
	public static final String XML_ATTRIBUTE_TARGET_EPOCH = "targetEpoch";
	public static final String XML_ATTRIBUTE_TARGET_EQUINOX = "targetEquinox";
	public static final String XML_ELEMENT_ALIGNMENT = "alignment";
	public static final String XML_ELEMENT_ALIGNMENT_SLIT = "alignSlit";
	public static final String XML_ATTRIBUTE_MECH_SLIT_NUMBER = "mechSlitNumber";
	public static final String XML_ELEMENT_MASCGEN_ARGUMENTS = "mascgenArguments";

	public static final String STATUS_NEW = "new";
	public static final String STATUS_MODIFIED = "modified";
	public static final String STATUS_SAVED = "saved";
	public static final String STATUS_UNSAVEABLE = "unsaveable";
	
	private String maskName;
	private String mscVersion;
	private String originalFilename;
	private ArrayList<MechanicalSlit> mechanicalSlitList;
	private ArrayList<ScienceSlit> scienceSlitList;
	private ArrayList<MechanicalSlit> alignSlitList;
	private MascgenArguments mascgenArgs;
	private MascgenResult mascgenResult;
	private List<AstroObj> originalTargetList;
	// Part of MAGMA UPGRADE M4 by Ji Man Sohn, UCLA 2016-2017
	private List<AstroObj> excessTargetList;
	private String status;
	private boolean hasObjectsIn0Ra = false;
	private boolean hasObjectsIn23Ra = false;

	private static SlitPositionSorter slitPositionSorter;
	private final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
	private final SAXBuilder builder = new SAXBuilder();
	private final DecimalFormat oneDigitFormatter = NumberFormatters.StandardFloatFormatter(1);  
	private final DecimalFormat twoDigitFormatter = NumberFormatters.StandardFloatFormatter(2);  
	private final DecimalFormat threeDigitFormatter = NumberFormatters.StandardFloatFormatter(3);  
	private final DecimalFormat fiveDigitFormatter = NumberFormatters.StandardFloatFormatter(5);
	private final DecimalFormat twoDigitWholeNumberFormatter = new DecimalFormat("00");
	private final DecimalFormat degreeSecondFormatter = new DecimalFormat("00.00");
  	

	/**
	 * Constructor for a unsaveable configuration.

	 * @param maskName   String name of the mask configuration
	 */
	public SlitConfigurationNEW(String maskName) {
		this(maskName, false);
	}

	/**
	 * General constructor for new configuration.
	 * 
	 * @param maskName  String name of mask configuration
	 * @param isNew     set to true for new modifiable configurations, false for unsaveable configurations
	 */
	public SlitConfigurationNEW(String maskName, boolean isNew) {
		this.maskName = maskName;
		
		//. initialize mechanical slits with centered slits with default width
		mechanicalSlitList = new ArrayList<MechanicalSlit>(MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS);
		for (int ii=0; ii<MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS; ii++) {
			mechanicalSlitList.add(new MechanicalSlit(ii+1, 0.0, MosfireParameters.DEFAULT_SLIT_WIDTH));
		}
		scienceSlitList = new ArrayList<ScienceSlit>(MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS);
		alignSlitList = new ArrayList<MechanicalSlit>(MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS);
		mascgenResult = new MascgenResult();
		mascgenArgs = new MascgenArguments();
		slitPositionSorter = new SlitPositionSorter();
		mscVersion="unknown";
		originalFilename="none";
		status = (isNew ? STATUS_NEW : STATUS_UNSAVEABLE);
	}

	/**
	 * Default constructor.  Creates unsaveable configuration with name "none".
	 */
	public SlitConfigurationNEW() {
		this("none");
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public SlitConfigurationNEW clone() {
		try {
			SlitConfigurationNEW newConfig = (SlitConfigurationNEW)super.clone();
			ArrayList<MechanicalSlit> newMechSlitList = new ArrayList<MechanicalSlit>(mechanicalSlitList.size());
			for (MechanicalSlit slit : mechanicalSlitList) {
				newMechSlitList.add(slit.clone());
			}
			newConfig.setMechanicalSlitList(newMechSlitList);

			ArrayList<ScienceSlit> newScienceSlitList = new ArrayList<ScienceSlit>(scienceSlitList.size());
			for (ScienceSlit slit : scienceSlitList) {
				newScienceSlitList.add(slit.clone());
			}
			newConfig.setScienceSlitList(newScienceSlitList);

			ArrayList<MechanicalSlit> newAlignSlitList = new ArrayList<MechanicalSlit>(alignSlitList.size());
			for (MechanicalSlit slit : alignSlitList) {
				newAlignSlitList.add(slit.clone());
			}
			newConfig.setAlignSlitList(newAlignSlitList);
			if (originalTargetList != null) {
				newConfig.setOriginalTargetSet(new ArrayList<AstroObj>(originalTargetList));
			}
			
			return newConfig;
		} catch (CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}
	
	/**
	 * Increment or decrement the slit width of all slits be the same amount.
	 * Slit widths cannot be less that the minimum slit width (defined by 
	 * <code>MSCGUIParameters.MINIMUM_SLIT_WIDTH</code>) and a slit cannot be
	 * made to have a bar going outside its limits, defined by 
	 * <code>MSCGUIParameters.CSU_MINIMUM_BAR_POSITION_MM</code> and 
	 * <code>MSCGUIParameters.CSU_MAXIMUM_BAR_POSITION_MM</code>.  If this would 
	 * happen due the applied offset, the adjustment is not made, and 
	 * a value of false is returned.
	 * 
	 * @param  offset  Double value to adjust slit width in arcsec
	 * @return         false if adjusting slit width cannot be performed, true otherwise.
	 */
	public boolean incrementSlitWidth(double offset) {
		double sideOffsetInMM = offset / MosfireParameters.CSU_ARCSEC_PER_MM / 2.0;
		//. don't allow if it would force slit off of FOV
		logger.trace("offset="+offset+", sideOffset="+sideOffsetInMM);
		for (MechanicalSlit mechSlit : mechanicalSlitList) {
			logger.trace("slit "+mechSlit.getSlitNumber()+": width="+mechSlit.getSlitWidth()+" left="+mechSlit.getLeftBarPositionInMM()+", right="+mechSlit.getRightBarPositionInMM());
			//. make sure offsets are allowed
			if ((mechSlit.getSlitWidth() + offset) < MosfireParameters.MINIMUM_SLIT_WIDTH) {
				return false;
			}
			if ((mechSlit.getLeftBarPositionInMM() + sideOffsetInMM) > MosfireParameters.CSU_MAXIMUM_BAR_POSITION_MM) {
				return false;
			}
			if ((mechSlit.getRightBarPositionInMM() - sideOffsetInMM) < MosfireParameters.CSU_MINIMUM_BAR_POSITION_MM) {
				return false;
			}
		}

		//. adjust slits in mechanical and science lists.
		mascgenArgs.setSlitWidth(mascgenArgs.getSlitWidth()+offset);
		for (MechanicalSlit slit : mechanicalSlitList) {
			slit.setSlitWidth(slit.getSlitWidth()+offset);
		}
		for (ScienceSlit slit : scienceSlitList) {
			slit.setSlitWidth(slit.getSlitWidth()+offset);
		}
		status = STATUS_MODIFIED;
		return true;
	}

	/**
	 * Set the slit width for a single science slit.
	 * A science slit must have the same width for all mechanical slits it comprises.
	 * If the requested slit width would create an invalid width,
	 * the slit width is adjusted to stay within the limits.
	 * 
	 * @param mechSlitNumber  the mechanical slit number containing the science slit to adjust
	 * @param slitWidth       the new desired width to set for science slit in arcsec
	 */
	public void setSlitWidth(int mechSlitNumber, double slitWidth) {

		//. get mech slit
		MechanicalSlit mechSlit = mechanicalSlitList.get(mechSlitNumber);
		
		//. get object in slit
		AstroObj obj = mechSlit.getTarget();
		//. get science slit for object (containing mech slit)
		ScienceSlit sslit = getScienceSlitWithAstroObj(obj);
		
		//. make sure mech slit is in a science slit (should usually be)
		if (sslit != null) {
			int scienceSlitRows = sslit.getSlitRows();
			
			//. get the range of rows science slit occupys
			int scienceSlitCenterRow = MascgenTransforms.getRowFromRaDec(sslit.getSlitRaDec(), mascgenResult.getCenter(), mascgenResult.getPositionAngle());
			int startRow;
			int endRow;
			if (scienceSlitRows % 2 == 0) {
				startRow = scienceSlitCenterRow - scienceSlitRows/2 + 1;
				endRow = scienceSlitCenterRow + scienceSlitRows/2;
			} else {
				startRow = scienceSlitCenterRow - (scienceSlitRows-1)/2;
				endRow = scienceSlitCenterRow + (scienceSlitRows-1)/2;
			}
			logger.debug("SlitConifguration setSlitWidth: startRow = "+startRow+", endRow = "+endRow);

			//. determine the min and max center positions for slit.
			double minCenterPosition = 999;
			double maxCenterPosition = -999;
			for (int ii=startRow-1; ii<endRow; ii++) {
				mechSlit = mechanicalSlitList.get(ii);
				if (mechSlit.getCenterPosition() < minCenterPosition) {
					minCenterPosition = mechSlit.getCenterPosition();
				}
				if (mechSlit.getCenterPosition() > maxCenterPosition) {
					maxCenterPosition = mechSlit.getCenterPosition();
				}
			}
			
			//. determine maximum allowed slit width (so slit doesn't go off FOV)
			double maxSlitWidth;
			if (Math.abs(minCenterPosition) > Math.abs(maxCenterPosition)) {
				maxSlitWidth = (CSU_WIDTH/2 - Math.abs(minCenterPosition)) * 2.0;
			} else {
				maxSlitWidth = (CSU_WIDTH/2 - Math.abs(maxCenterPosition)) * 2.0;				
			}
			
			//. limit slit width by minimum and maximum amounts
			if (slitWidth < MosfireParameters.MINIMUM_SLIT_WIDTH) {
				slitWidth = MosfireParameters.MINIMUM_SLIT_WIDTH;
			} else if (slitWidth > maxSlitWidth) {
				slitWidth = maxSlitWidth;
			}

			//. set slit width for all mechanical slits in science slit
			for (int ii=startRow-1; ii<endRow; ii++) {
				mechSlit = mechanicalSlitList.get(ii);
				mechSlit.setSlitWidth(slitWidth);
			}

			//. set science slit width
			sslit.setSlitWidth(slitWidth);

			status = STATUS_MODIFIED;
		}
	}
	
	/**
	 * Generate a long slit configuration of specified length in rows with specified width in arcsec.
	 * An alignment box is placed in the middle of the slit.
	 * Configuration name is LONGSLIT-<length>x<width>.
	 * 
	 * @param  slitLength  length of slit in rows
	 * @param  slitWidth   width of slit in arcsec.
	 * @return             SlitConfiguration for long slit configuration.
	 */
	public static SlitConfigurationNEW createLongSlitConfiguration(int slitLength, double slitWidth) {
		DecimalFormat formatter = new DecimalFormat("0.######");
		SlitConfigurationNEW config = new SlitConfigurationNEW("LONGSLIT-"+slitLength+"x"+formatter.format(slitWidth));
		config.setStatus(STATUS_UNSAVEABLE);
		ArrayList<MechanicalSlit> newMechanicalSlitList = new ArrayList<MechanicalSlit>(slitLength);
		int row;
		double rowCenterOffInArcsec;
		double slitCenterArcsec;
		int halfRows = (int)Math.ceil(slitLength/2.0)-1;
		for (int ii=0; ii<slitLength; ii++) {
			//. row, 1-based
			//. rows go positive down, but in arcsec, goes positive up, and to the left
			//. bar positions in mm go positive to the left
			
			row = MosfireParameters.CSU_CALIBRATION_SLIT_MIDDLE_ROW - halfRows + ii;
			rowCenterOffInArcsec = (halfRows - ii) * MosfireParameters.SINGLE_SLIT_HEIGHT;
			slitCenterArcsec = Math.tan(MosfireParameters.CSU_SLIT_TILT_ANGLE_RADIANS)* rowCenterOffInArcsec;
			newMechanicalSlitList.add(new MechanicalSlit(row, slitCenterArcsec, slitWidth));
		}
		config.setMechanicalSlitList(newMechanicalSlitList);
		ArrayList<MechanicalSlit> alignList = config.getAlignSlitList();
		alignList.add(new MechanicalSlit(MosfireParameters.CSU_CALIBRATION_SLIT_MIDDLE_ROW, newMechanicalSlitList.get(halfRows).getCenterPosition(), MosfireParameters.ALIGNMENT_BOX_SLIT_WIDTH));
		config.getMascgenArgs().setSlitWidth(slitWidth);
		config.setOriginalFilename(config.getMaskName());
		return config;
	}

	/**
	 * Create an open mask configuration.  
	 * Bar positions are defined by MosfireParameters.CSU_OPEN_BAR_TARGETS.
	 * 
	 * @return  SlitConfiguration for open mask
	 */
	public static SlitConfigurationNEW createOpenMaskSlitConfiguration() {
		SlitConfigurationNEW config = new SlitConfigurationNEW("OPEN");
		config.setStatus(STATUS_UNSAVEABLE);
		ArrayList<MechanicalSlit> newMechanicalSlitList = new ArrayList<MechanicalSlit>(MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS);
		double slitWidth;
		double centerPosition;
		double oddBarTarg, evenBarTarg;
		for (int ii=0; ii<MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS; ii++) {
			oddBarTarg = MosfireParameters.CSU_OPEN_BAR_TARGETS[ii*2];
			evenBarTarg = MosfireParameters.CSU_OPEN_BAR_TARGETS[ii*2+1];
			slitWidth = (evenBarTarg - oddBarTarg) * MosfireParameters.CSU_ARCSEC_PER_MM;
			centerPosition = ((evenBarTarg + oddBarTarg)/2.0 - MosfireParameters.CSU_ZERO_PT) * MosfireParameters.CSU_ARCSEC_PER_MM;
			
			newMechanicalSlitList.add(new MechanicalSlit(ii+1, centerPosition, slitWidth));
		}
		config.setMechanicalSlitList(newMechanicalSlitList);
		config.getMascgenArgs().setSlitWidth(MosfireParameters.CSU_OPEN_MASK_SLIT_WIDTH);
		return config;
	}
	
	/**
	 * Get slit position for specified index.
	 * Index is 0-based index of slit in mechanical slit list.
	 * Position is obtained from mechanical slit list.
	 * 
	 * @param  row                             index of slit 
	 * @return                                 SlitPosition for index
	 * @throws ArrayIndexOutOfBoundsException  if index is outside of range for mechanical slit list
	 */
	public SlitPosition getSlitPosition(int row) throws ArrayIndexOutOfBoundsException {
		//. 0-based
		return mechanicalSlitList.get(row);
	}
	
	/**
	 * Read MOSFIRE Slit Configuration file and populate this configuration with specification in file.
	 * 
	 * @param  listFile        File object pointing to MSC file
	 * @param  outWarningList  String ArrayList to receive warnings found with MSC
	 * @throws JDOMException   on error parsing MSC
	 * @throws IOException     on error opening MSC file
	 */
	@SuppressWarnings("unchecked")
	//. JDOM doesn't support generics, but getChidren returns a List of Elements
	public void readSlitConfiguration(File listFile, ArrayList<String> outWarningList) throws JDOMException, IOException {
		String newVersion = "unknown";
		Document myDoc = builder.build(listFile);

		Attribute workingAtt;
		//. get root element.
		Element root=myDoc.getRootElement();

		//. check that it has the proper name
		if (root.getName().compareTo(XML_ROOT) != 0)
			throw new JDOMException("Root element must be "+XML_ROOT);

		//. get version
		workingAtt = root.getAttribute(XML_ATTRIBUTE_MSC_VERSION);
		if (workingAtt != null) {
			newVersion = workingAtt.getValue();
		}
		
		List<Element> subElements;

		boolean maskDescriptionFound = false;
		boolean mechanicalSlitsFound = false;
		boolean scienceSlitsFound = false;

		//. initialize this configuration
		String newMaskName = "";
		MascgenResult newMascgenResult = new MascgenResult();
		ArrayList<MechanicalSlit> newSlitList = new ArrayList<MechanicalSlit>(MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS);
		ArrayList<ScienceSlit> newScienceSlitList = new ArrayList<ScienceSlit>(MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS);
		ArrayList<MechanicalSlit> newAlignList = new ArrayList<MechanicalSlit>(MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS);
		MascgenArguments newMascgenArgs = new MascgenArguments();
		status = STATUS_NEW;

		if (originalTargetList != null) {
			originalTargetList.clear();
		}
		hasObjectsIn0Ra = false;
		hasObjectsIn23Ra = false;
		
		//. get children elements
		List<Element> elements= (List<Element>)(root.getChildren());

		//. loop through them
		for (Element current : elements) {

			double center, width;
			int number;

			//. begin MASK DESCRIPTION
			if (current.getName().equals(XML_ELEMENT_MASK_DESCRIPTION)) {
				maskDescriptionFound = true;

				workingAtt = current.getAttribute(XML_ATTRIBUTE_MASK_NAME);
				if (workingAtt != null) {
					newMaskName = workingAtt.getValue();
				} else {
					throw new JDOMException(XML_ELEMENT_MASK_DESCRIPTION+" elements must have a "+XML_ATTRIBUTE_MASK_NAME+" attribute");
				}
				workingAtt = current.getAttribute(XML_ATTRIBUTE_TOTAL_PRIORITY);
				if (workingAtt != null) {
					newMascgenResult.setTotalPriority(workingAtt.getDoubleValue());
				} else {
					throw new JDOMException(XML_ELEMENT_MASK_DESCRIPTION+" elements must have a "+XML_ATTRIBUTE_TOTAL_PRIORITY+" attribute");
				}
				workingAtt = current.getAttribute(XML_ATTRIBUTE_CENTER_RAH);
				if (workingAtt != null) {
					newMascgenResult.getCenter().setRaHour(workingAtt.getIntValue());
				} else {
					throw new JDOMException(XML_ELEMENT_MASK_DESCRIPTION+" elements must have a "+XML_ATTRIBUTE_CENTER_RAH+" attribute");
				}
				workingAtt = current.getAttribute(XML_ATTRIBUTE_CENTER_RAM);
				if (workingAtt != null) {
					newMascgenResult.getCenter().setRaMin(workingAtt.getIntValue());
				} else {
					throw new JDOMException(XML_ELEMENT_MASK_DESCRIPTION+" elements must have a "+XML_ATTRIBUTE_CENTER_RAM+" attribute");
				}
				workingAtt = current.getAttribute(XML_ATTRIBUTE_CENTER_RAS);
				if (workingAtt != null) {
					newMascgenResult.getCenter().setRaSec(workingAtt.getDoubleValue());
				} else {
					throw new JDOMException(XML_ELEMENT_MASK_DESCRIPTION+" elements must have a "+XML_ATTRIBUTE_CENTER_RAS+" attribute");
				}
				workingAtt = current.getAttribute(XML_ATTRIBUTE_CENTER_DECD);
				if (workingAtt != null) {
					newMascgenResult.getCenter().setDecDeg(workingAtt.getDoubleValue());
				} else {
					throw new JDOMException(XML_ELEMENT_MASK_DESCRIPTION+" elements must have a "+XML_ATTRIBUTE_CENTER_DECD+" attribute");
				}
				workingAtt = current.getAttribute(XML_ATTRIBUTE_CENTER_DECM);
				if (workingAtt != null) {
					newMascgenResult.getCenter().setDecMin(workingAtt.getDoubleValue());
				} else {
					throw new JDOMException(XML_ELEMENT_MASK_DESCRIPTION+" elements must have a "+XML_ATTRIBUTE_CENTER_DECM+" attribute");
				}
				workingAtt = current.getAttribute(XML_ATTRIBUTE_CENTER_DECS);
				if (workingAtt != null) {
					newMascgenResult.getCenter().setDecSec(workingAtt.getDoubleValue());
				} else {
					throw new JDOMException(XML_ELEMENT_MASK_DESCRIPTION+" elements must have a "+XML_ATTRIBUTE_CENTER_DECS+" attribute");
				}
				workingAtt = current.getAttribute(XML_ATTRIBUTE_MASK_PA);
				if (workingAtt != null) {
					newMascgenResult.setPositionAngle(workingAtt.getDoubleValue());
				} else {
					throw new JDOMException(XML_ELEMENT_MASK_DESCRIPTION+" elements must have a "+XML_ATTRIBUTE_MASK_PA+" attribute");
				}
				//. end MASK DESCRIPTION, begin MECHANICAL SLIT
			} else if (current.getName().equals(XML_ELEMENT_MECHANICAL_SLIT_CONFIG)) {
				mechanicalSlitsFound = true;

				subElements = (List<Element>)(current.getChildren());

				for (Element subCurrent : subElements) {		

					if (subCurrent.getName().equals(XML_ELEMENT_MECHANICAL_SLIT)) {

						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_NUMBER);
						if (workingAtt != null) {
							number = workingAtt.getIntValue();
						} else {
							throw new JDOMException(XML_ELEMENT_MECHANICAL_SLIT + " elements must have a "+XML_ATTRIBUTE_SLIT_NUMBER+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_CENTER_POSITION);
						if (workingAtt != null) {
							center = workingAtt.getDoubleValue();
						} else {
							throw new JDOMException(XML_ELEMENT_MECHANICAL_SLIT + " elements must have a "+XML_ATTRIBUTE_CENTER_POSITION+" attribute");
						}

						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_WIDTH);
						if (workingAtt != null) {
							width = workingAtt.getDoubleValue();
						} else {
							throw new JDOMException(XML_ELEMENT_MECHANICAL_SLIT + " elements must have a "+XML_ATTRIBUTE_SLIT_WIDTH+" attribute");
						}
						MechanicalSlit newSlitPos = new MechanicalSlit(number, center, width);

						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET);
						if (workingAtt != null) {
							newSlitPos.setTargetName(workingAtt.getValue());
						} else {
							outWarningList.add(newSlitPos+" does not have a target tag.");
						}

						newSlitList.add(newSlitPos);
					} else {
						throw new JDOMException(subCurrent.getName()+" is not a valid child element of "+XML_ELEMENT_MECHANICAL_SLIT_CONFIG+" elements.");
					}
				} //. end loop over mechanicalSlitConfig elements
				//. end MECHANICAL SLITS begin SCIENCE SLITS
			} else if (current.getName().equals(XML_ELEMENT_SCIENCE_SLIT_CONFIG)) {
				scienceSlitsFound = true;

				subElements = (List<Element>)(current.getChildren());

				for (Element subCurrent : subElements) {		

					if (subCurrent.getName().equals(XML_ELEMENT_SCIENCE_SLIT)) {

						ScienceSlit newSlit = new ScienceSlit();

						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_NUMBER);
						if (workingAtt != null) {
							newSlit.setSlitNumber(workingAtt.getIntValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_SLIT_NUMBER+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_RAH);
						if (workingAtt != null) {
							newSlit.getSlitRaDec().setRaHour(workingAtt.getIntValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_SLIT_RAH+" attribute (slit number="+newSlit.getSlitNumber()+")");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_RAM);
						if (workingAtt != null) {
							newSlit.getSlitRaDec().setRaMin(workingAtt.getIntValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_SLIT_RAM+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_RAS);
						if (workingAtt != null) {
							newSlit.getSlitRaDec().setRaSec(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_SLIT_RAS+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_DECD);
						if (workingAtt != null) {
							newSlit.getSlitRaDec().setDecDeg(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_SLIT_DECD+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_DECM);
						if (workingAtt != null) {
							newSlit.getSlitRaDec().setDecMin(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_SLIT_DECM+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_DECS);
						if (workingAtt != null) {
							newSlit.getSlitRaDec().setDecSec(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_SLIT_DECS+" attribute");
						}

						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_WIDTH);
						if (workingAtt != null) {
							newSlit.setSlitWidth(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_SLIT_WIDTH+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_LENGTH);
						if (workingAtt != null) {
							newSlit.setSlitLength(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_SLIT_LENGTH+" attribute");
						}

						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET);
						if (workingAtt != null) {
							newSlit.getTarget().setObjName(workingAtt.getValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_PRIORITY);
						if (workingAtt != null) {
							newSlit.getTarget().setObjPriority(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_PRIORITY+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_MAGNITUDE);
						if (workingAtt != null) {
							newSlit.getTarget().setObjMag(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_MAGNITUDE+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_CENTER_DISTANCE);
						if (workingAtt != null) {
							newSlit.setCenterDistance(workingAtt.getDoubleValue());
							newSlit.getTarget().setCenterDistance(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_CENTER_DISTANCE+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_RAH);
						if (workingAtt != null) {
							newSlit.getTarget().setRaHour(workingAtt.getIntValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_RAH+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_RAM);
						if (workingAtt != null) {
							newSlit.getTarget().setRaMin(workingAtt.getIntValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_RAM+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_RAS);
						if (workingAtt != null) {
							newSlit.getTarget().setRaSec(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_RAS+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_DECD);
						if (workingAtt != null) {
							newSlit.getTarget().setDecDeg(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_DECD+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_DECM);
						if (workingAtt != null) {
							newSlit.getTarget().setDecMin(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_DECM+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_DECS);
						if (workingAtt != null) {
							newSlit.getTarget().setDecSec(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_DECS+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_EPOCH);
						if (workingAtt != null) {
							newSlit.getTarget().setEpoch(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_EPOCH+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_EQUINOX);
						if (workingAtt != null) {
							newSlit.getTarget().setEquinox(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_SCIENCE_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_EQUINOX+" attribute");
						}

						newScienceSlitList.add(newSlit);
					} else {
						throw new JDOMException(subCurrent.getName()+" is not a valid child element of "+XML_ELEMENT_SCIENCE_SLIT_CONFIG+" elements.");
					}
				}  //. end loop over ScienceSlitConfig elements
				//. end SCIENCE SLITS begin ALIGNMENT
			} else if (current.getName().equals(XML_ELEMENT_ALIGNMENT)) {
				subElements = (List<Element>)(current.getChildren());

				for (Element subCurrent : subElements) {		

					if (subCurrent.getName().equals(XML_ELEMENT_ALIGNMENT_SLIT)) {

						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_MECH_SLIT_NUMBER);
						if (workingAtt != null) {
							number = workingAtt.getIntValue();
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_MECH_SLIT_NUMBER+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_CENTER_POSITION);
						if (workingAtt != null) {
							center = workingAtt.getDoubleValue();
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_CENTER_POSITION+" attribute");
						}

						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_SLIT_WIDTH);
						if (workingAtt != null) {
							width = workingAtt.getDoubleValue();
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_SLIT_WIDTH+" attribute");
						}
						MechanicalSlit newSlit = new MechanicalSlit(number, center, width);

						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET);
						if (workingAtt != null) {
							newSlit.setTargetName(workingAtt.getValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET+" attribute");
						}

						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_PRIORITY);
						if (workingAtt != null) {
							newSlit.getTarget().setObjPriority(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_PRIORITY+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_MAGNITUDE);
						if (workingAtt != null) {
							newSlit.getTarget().setObjMag(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_MAGNITUDE+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_CENTER_DISTANCE);
						if (workingAtt != null) {
							newSlit.setCenterDistance(workingAtt.getDoubleValue());
							newSlit.getTarget().setCenterDistance(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_CENTER_DISTANCE+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_RAH);
						if (workingAtt != null) {
							newSlit.getTarget().setRaHour(workingAtt.getIntValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_RAH+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_RAM);
						if (workingAtt != null) {
							newSlit.getTarget().setRaMin(workingAtt.getIntValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_RAM+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_RAS);
						if (workingAtt != null) {
							newSlit.getTarget().setRaSec(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_RAS+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_DECD);
						if (workingAtt != null) {
							newSlit.getTarget().setDecDeg(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_DECD+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_DECM);
						if (workingAtt != null) {
							newSlit.getTarget().setDecMin(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_DECM+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_DECS);
						if (workingAtt != null) {
							newSlit.getTarget().setDecSec(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_DECS+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_EPOCH);
						if (workingAtt != null) {
							newSlit.getTarget().setEpoch(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_EPOCH+" attribute");
						}
						workingAtt = subCurrent.getAttribute(XML_ATTRIBUTE_TARGET_EQUINOX);
						if (workingAtt != null) {
							newSlit.getTarget().setEquinox(workingAtt.getDoubleValue());
						} else {
							throw new JDOMException(XML_ELEMENT_ALIGNMENT_SLIT+" elements must have a "+XML_ATTRIBUTE_TARGET_EQUINOX+" attribute");
						}


						newAlignList.add(newSlit);
					} else {
						throw new JDOMException(subCurrent.getName()+" is not a valid child element of "+XML_ELEMENT_ALIGNMENT+" elements.");
					}
				}  //. end for loop on alignment elements
				//. end ALIGNMENT begin MASCGEN ARGUMENTS
			} else if (current.getName().equals(XML_ELEMENT_MASCGEN_ARGUMENTS)) {
				newMascgenArgs = MascgenArguments.getMascgenArgsFromElement(current, outWarningList);
				//. end MASCGEN ARGUMENTS
			} else {
				throw new JDOMException(current.getName()+" is not a valid child element of the "+XML_ROOT+" element.");
			}


		}  //. end loop over all elements

		if (!maskDescriptionFound) {
			throw new JDOMException("Required element "+XML_ELEMENT_MASK_DESCRIPTION+" not found.");
		}
		if (!mechanicalSlitsFound) {
			throw new JDOMException("Required element "+XML_ELEMENT_MECHANICAL_SLIT_CONFIG+" not found.");
		}
		if (!scienceSlitsFound) {
			throw new JDOMException("Required element "+XML_ELEMENT_SCIENCE_SLIT_CONFIG+" not found.");
		}

		//. if we made it this far, no issues, go ahead and set all new members
	  setVersion(newVersion);
	  setMaskName(newMaskName);
		setMechanicalSlitList(newSlitList);
		setScienceSlitList(newScienceSlitList);
		setAlignSlitList(newAlignList);
		setMascgenArgs(newMascgenArgs);
		
		setMascgenResult(newMascgenResult);

		AstroObj[] objs = getAstroObjectListFromScienceSlitList();
		AstroObj[] stars = getStarAstroObjectListFromAlignSlitList();
		
		//. set if objects are wrapped around 0 RA
		//. hasObjectsIn0Ra & hasObjectsIn23Ra will be set in above two functions
		mascgenResult.setCoordWrap(hasObjectsIn0Ra && hasObjectsIn23Ra);
		
		RaDec centerPosition = newMascgenResult.getCenter();

		//. apply coord wrap in order to get XY position of center
		//. then undo
		if (mascgenResult.isCoordWrap()) {
			MascgenTransforms.applyRaCoordWrap(centerPosition);
		}
		MascgenTransforms.raDecToXY(centerPosition);
		
		if (mascgenResult.isCoordWrap()) {
			MascgenTransforms.fixRaCoordWrap(centerPosition);
		}
		
		//. update csu xy and wcs in astro objs according to pointing
		updateAstroObjects();
		
		//. put astro objects in result
		mascgenResult.setAstroObjects(objs);
		mascgenResult.setLegalAlignmentStars(stars);
		
		//. update targets in mechanical list
		updateMechanicalListTargets();
		
		originalFilename = listFile.getAbsolutePath();
		status =  STATUS_SAVED;
	}

	/**
	 * Set the proper targets for each mechanical slit.
	 * An object may be in more than on mechanical slit,
	 * but slits must be contiguous.
	 */
	private void updateMechanicalListTargets() {
		//. keep reference to last object used
		AstroObj lastObject = new AstroObj();
		//. keep reference to the first slit with the current target
		MechanicalSlit firstTargetSlit = new MechanicalSlit(0);

		//. go through slits.
		//. slits will have target names, but no actual AstroObjs
		//. from name, get AstroObj, but if the name is the same
		//. as the previous slit, use the AstroObj from that slit.
		//. set slit rows properly, which is the length of the corresponding
		//. science slit.
		for (MechanicalSlit slit : mechanicalSlitList) {
			if (slit.getTargetName().equals(lastObject.getObjName())) {
				slit.setTarget(lastObject);
				firstTargetSlit.setSlitRows(firstTargetSlit.getSlitRows() + 1);
			} else {
				AstroObj target = findAstroObj(slit.getTarget());
				slit.setTarget(target);
				slit.setSlitRows(1);
				firstTargetSlit = slit;
				lastObject = target;
			}
		}
	}
	
	/**
	 * Sum up priority scores of targets in valid slits and set in <code>MascgenResult</code>
	 */
	private void updatePriority() {
		double priority = 0;
		for (ScienceSlit slit : scienceSlitList) {
			if (slit.getTarget().isInValidSlit()) {
				priority += slit.getTarget().getObjPriority();
			}
		}
		mascgenResult.setTotalPriority(priority);
	}

	/**
	 * Determine if any of the slits are invalid for this configuration.
	 * A slit is invalid if the target is not far enough from edge of slit
	 * as defined by the dither space parameters in the <code>MascgenArgs</code> object.
	 * 
	 * @return  true if any slits are invalid, false if not
	 */
	public boolean hasInvalidSlits() {
		for (ScienceSlit slit : scienceSlitList) {
			if (!slit.getTarget().isInValidSlit()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Find a particular <code>AstroObj</code> from list of objects in <code>MascgenResult</code>.
	 * @param obj
	 * @return
	 */
	private AstroObj findAstroObj(AstroObj obj) {
		for (AstroObj currentObj : mascgenResult.getAstroObjects()) {
			if (obj.getObjName().equals(currentObj.getObjName())) {
				return currentObj;
			}
		}
		return obj;
	}
	
	/**
	 * Gets the list of targets from science slit list.
	 * Checks for coordinate wrap is it goes.
	 *
	 * @return AstroObj array from science slit list
	 */
	private AstroObj[] getAstroObjectListFromScienceSlitList() {
		AstroObj[] astroObjs = new AstroObj[scienceSlitList.size()];
		int ii=0;
		for(ScienceSlit slit : scienceSlitList) {
			 AstroObj obj = slit.getTarget();
			 astroObjs[ii] = obj;
			 if (obj.getRaHour() == 0) {
				hasObjectsIn0Ra = true;
			} else if (obj.getRaHour() == 23) {
				hasObjectsIn23Ra = true;
			}
			ii++;
		}
		return astroObjs;
	}
	
	/**
	 * Update astro objects with CSU XY and WCS information based on current centering.
	 */
	public void updateAstroObjects() {
		RaDec centerPosition = mascgenResult.getCenter();
		double theta = Math.toRadians(mascgenResult.getPositionAngle());
		
		double xOld, yOld;
		for(ScienceSlit slit : scienceSlitList) {
			AstroObj obj = slit.getTarget();
			//. if coordinate wrap around 0 RA, apply to center position
			if (mascgenResult.isCoordWrap()) {
				MascgenTransforms.applyRaCoordWrap(obj);
			}
			MascgenTransforms.astroObjRaDecToXY(obj, centerPosition);
			if (mascgenResult.isCoordWrap()) {
				MascgenTransforms.fixRaCoordWrap(obj);
			}
			//. rotate objects
			xOld = obj.getWcsX() - centerPosition.getXCoordinate();
			yOld = obj.getWcsY() - centerPosition.getYCoordinate();
			obj.setObjX(xOld * Math.cos(theta) - yOld * Math.sin(theta) + CSU_WIDTH / 2);
			obj.setObjY(xOld * Math.sin(theta) + yOld * Math.cos(theta) + CSU_HEIGHT / 2);
			obj.setInitialRR(mascgenArgs.getDitherSpace());
			obj.setInitialOR(mascgenArgs.getDitherSpace());
			obj.setObjX(obj.getObjX() - CSU_WIDTH/2);
			obj.setObjY(obj.getObjY() - CSU_HEIGHT/2);
			obj.updateDitherRows(mascgenArgs.getDitherSpace());
			obj.setInValidSlit(Math.abs(slit.getCenterDistance()) < (slit.getSlitLength()/2 - mascgenArgs.getDitherSpace()));
		}

		//. apply to alignment objects
		for(MechanicalSlit slit : alignSlitList) {
			AstroObj obj = slit.getTarget();
			if (mascgenResult.isCoordWrap()) {
				MascgenTransforms.applyRaCoordWrap(obj);
			}
			MascgenTransforms.astroObjRaDecToXY(obj, centerPosition);
			if (mascgenResult.isCoordWrap()) {
				MascgenTransforms.fixRaCoordWrap(obj);
			}
			xOld = obj.getWcsX() - centerPosition.getXCoordinate();
			yOld = obj.getWcsY() - centerPosition.getYCoordinate();
			obj.setObjX(xOld * Math.cos(theta) - yOld * Math.sin(theta) + CSU_WIDTH / 2);
			obj.setObjY(xOld * Math.sin(theta) + yOld * Math.cos(theta) + CSU_HEIGHT / 2);
			obj.setInitialRR(mascgenArgs.getAlignmentStarEdgeBuffer());
			obj.setObjX(obj.getObjX() - CSU_WIDTH/2);
			obj.setObjY(obj.getObjY() - CSU_HEIGHT/2);
		}

	}

	/**
	 * Update astro objects in original target list with CSU XY and WCS
	 */
	public void updateOriginalAstroObjects() {
		RaDec centerPosition = mascgenResult.getCenter();
		double theta = Math.toRadians(mascgenResult.getPositionAngle());

		//. first determine if coord wrap is used
		if (!mascgenResult.isCoordWrap()) {
			for (AstroObj obj : originalTargetList) {
				if (obj.getRaHour() == 0) {
					hasObjectsIn0Ra = true;
				} else if (obj.getRaHour() == 23) {
					hasObjectsIn23Ra = true;
				}
			}			
			mascgenResult.setCoordWrap(hasObjectsIn0Ra && hasObjectsIn23Ra);
			
			//. if we are now using coord wrap, update the center position
			//. and AstroObjects we are using
			if (mascgenResult.isCoordWrap()) {
				MascgenTransforms.applyRaCoordWrap(centerPosition);
			}
			MascgenTransforms.raDecToXY(centerPosition);
			
			if (mascgenResult.isCoordWrap()) {
				MascgenTransforms.fixRaCoordWrap(centerPosition);
				updateAstroObjects();
			}
		}


		double xOld, yOld;
		for(AstroObj obj : originalTargetList) {
			if (mascgenResult.isCoordWrap()) {
				MascgenTransforms.applyRaCoordWrap(obj);
			}
			MascgenTransforms.astroObjRaDecToXY(obj, centerPosition);
			if (mascgenResult.isCoordWrap()) {
				MascgenTransforms.fixRaCoordWrap(obj);
			}
			xOld = obj.getWcsX() - centerPosition.getXCoordinate();
			yOld = obj.getWcsY() - centerPosition.getYCoordinate();
			obj.setObjX(xOld * Math.cos(theta) - yOld * Math.sin(theta));
			obj.setObjY(xOld * Math.sin(theta) + yOld * Math.cos(theta));
			obj.updateDitherRows(mascgenArgs.getDitherSpace());
		}
	}
	
	/**
	 * Gets the star astro object list from align slit list.
	 * Checks for coordinate wrap as it goes.
	 * 
	 * @return AstroObj array of objects list from align slit list
	 */
	private AstroObj[] getStarAstroObjectListFromAlignSlitList() {
		AstroObj[] astroObjs = new AstroObj[alignSlitList.size()];
		
		int ii=0;
		for(MechanicalSlit slit : alignSlitList) {
			AstroObj obj = slit.getTarget();
			astroObjs[ii] = obj;
			if (obj.getRaHour() == 0) {
				hasObjectsIn0Ra = true;
			} else if (obj.getRaHour() == 23) {
				hasObjectsIn23Ra = true;
			}
			ii++;
		}

		return astroObjs;
	}

	/**
	 * Gets the science slit containing specified target.
	 *
	 * @param  obj  AstroObj to search for
	 * @return      ScienceSlit containing <code>obj</code>.  Returns null if object cannot be found.
	 */
	private ScienceSlit getScienceSlitWithAstroObj(AstroObj obj) {
		for (ScienceSlit slit : scienceSlitList) {
			if (slit.getTarget().equals(obj)) {
				return slit;
			}
		}
		return null;
	}

	/**
	 * Prints the slit number, slit rows, target name, and center position for the specified mechanical slit positions.
	 *
	 * @param slits  MechanicalSlit Array for which to print the information
	 */
	public static void printSlitPositions(MechanicalSlit[] slits) {
		for (MechanicalSlit pos : slits) {
			System.out.println(
					pos.getSlitNumber()
					+"\tR"+pos.getSlitRows()
					+"\t"+pos.getTargetName()
					+"\tCP"+pos.getCenterPosition()
			);
		}
	}

	/**
	 * Prints the name, priority, CSU X and Y, WCS X and Y, and row regions for specified targets.
	 *
	 * @param objs  AstroObj array for which to print the information
	 */
	public static void printAstroObjs(AstroObj[] objs) {
		for (AstroObj obj : objs) {
			System.out.println(obj.getObjName()+
					"\tP:"+obj.getObjPriority()+
					"\tX:"+obj.getObjX()+
					"\tY:"+obj.getObjY()+
					"\tWX:"+obj.getWcsX()+
					"\tWY:"+obj.getWcsY()+
					"\tRR: "+obj.getObjRR()+
					"\tOR: "+obj.getObjOR());
		}
	}

	/**
	 * Find best alignment set for given <code>MascgenResult</code> and <code>MascgenArguments</code>.
	 *
	 * @param  result MascgenResult containing valid alignment stars and pointing info
	 * @param  args   MascgenArguments object containing number of minimum alignment stars and star edge buffer
	 * @return        AstroObj array with alignment stars
	 */
	private static AstroObj[] findBestAlignmentSet(MascgenResult result, MascgenArguments args) {
		int minimumAlignmentStars = args.getMinimumAlignmentStars();
		AstroObj[] validStars = result.getLegalAlignmentStars();
		AstroObj[] winningAlignmentSet = new AstroObj[0];

		RaDec centerPosition = result.getCenter();
		RaDec objectRaDec;
		Point2D.Double objWcs;
		int row;
		double theta = Math.toRadians(result.getPositionAngle());

		double xOld, yOld;
		double objX, objY;

		//. update object values for this result
		for  (AstroObj obj : validStars) {

			if (result.isCoordWrap()) {
				if (obj.getRaHour() == 0) {
					obj.setRaHour(12);
				}	
				if (obj.getRaHour() == 23) {
					obj.setRaHour(11);
				}
			}

			// Transform the entire astroObjArray into the CSU plane by subtracting
			// the center coordinate from each AstroObj's xCoordinate and
			// yCoordinate and putting these into the ObjX and ObjY.
			objectRaDec = new RaDec((int)Math.floor(obj.getRaHour()), (int)Math.floor(obj.getRaMin()), obj.getRaSec(), obj.getDecDeg(), obj.getDecMin(), obj.getDecSec());
			objWcs = MascgenTransforms.getWcsFromRaDec(objectRaDec, centerPosition.getYCoordinate());

			if (result.isCoordWrap()) {
				MascgenTransforms.applyRaCoordWrap(obj);
			}

			
			xOld = objWcs.x - centerPosition.getXCoordinate();
			yOld = objWcs.y - centerPosition.getYCoordinate();
			
			// Rotate the objects in the CSU plane by the Position Angle.
			/* Objects were read in with coordinate system origin at center of
			 *  CSU field. The optimize method runs with the coordinate system
			 *  origin in the lower left. So, simply add CSU_WIDTH / 2 to the x
			 *  position and CSU_HEIGHT / 2 to the y position of each object. */
			objX = xOld * Math.cos(theta) - yOld * Math.sin(theta) + CSU_WIDTH / 2;
			objY = xOld * Math.sin(theta) + yOld * Math.cos(theta) + CSU_HEIGHT / 2;
			
			row=AstroObj.getRow(objY, args.getAlignmentStarEdgeBuffer());
			obj.setObjRR(row);
			obj.setObjX(objX - CSU_WIDTH / 2);
			obj.setObjY(objY - CSU_HEIGHT / 2);
			MascgenTransforms.astroObjRaDecToXY(obj, centerPosition);
			if (result.isCoordWrap()) {
				MascgenTransforms.fixRaCoordWrap(obj);
			}
		}
		//. create a combination generator for N choose K, where N is the total number of stars in the
		//. star list, and K is the number of stars used for alignment
		CombinationGenerator combgen = new CombinationGenerator(validStars.length, minimumAlignmentStars);
		int[] indices;
		AstroObj[] currentObjects;
		//. hash set used for making sure stars are not on same row
		HashSet<Integer> rows = new HashSet<Integer>(minimumAlignmentStars);

		//. initialize distance variables, used for determining best set
		double winningDistance = 0.0;
		double distance, minDistance, newDistance;

		//. go through all possible combinations
		while (combgen.hasMore()) {
			//. reset hash list
			rows.clear();
			//. get current combination indices
			indices = combgen.getNext();
			currentObjects = new AstroObj[indices.length];
			for (int ii=0; ii<indices.length; ii++) {
				currentObjects[ii] = validStars[indices[ii]];
				rows.add(new Integer(currentObjects[ii].getObjRR()));
			}

			//. the set with the maximum pairwise distance will be the "best" one
			//. this algorithm came from Gwen Rudie

			//. calculate pairwise distance
			distance = 0;
			minDistance = Double.MAX_VALUE;
			int jj;
			for (int ii=0; ii < currentObjects.length; ii++ ){
				jj = ii+1;
				while (jj < currentObjects.length){
					//. compute distance between two current points
					newDistance = Point.distance(currentObjects[ii].getObjX(), currentObjects[ii].getObjY(), currentObjects[jj].getObjX(), currentObjects[jj].getObjY());
					//. add to total distance for current set of stars
					distance += newDistance;

					//. keep track of smallest distance
					if (newDistance < minDistance){
						minDistance = newDistance;
					}

					jj++;
				}
			}
			//. weight total distance by minimum distance
			distance += minDistance*currentObjects.length;

			//. make sure they are on unique rows
			if ((rows.size() == minimumAlignmentStars) && (distance > winningDistance)) {
				//. save the best set
				winningDistance = distance;
				winningAlignmentSet = currentObjects;
			}
		}

		return winningAlignmentSet;
	}
	
	/**
	 * Generate slit configuration from specified  <code>MascgenResult</code> and <code>MascgenArguments</code>.
	 *
	 * @param args                 MascgenArguments object specifying arguments used to create results
	 * @param result               MascgenResults object with results from MASCGEN
	 * @param reassignUnusedSlits  Flag for whether or not to reassigned unused slits to neighbors
	 * @return                     SlitConfiguration object for this solution
	 */
	public static SlitConfigurationNEW generateSlitConfiguration(MascgenArguments args, MascgenResult result, boolean reassignUnusedSlits) {
		SlitConfigurationNEW config = new SlitConfigurationNEW(args.getMaskName(), true);
		config.setMascgenArgs(args);
		config.setMascgenResult(result);
		RaDec centerPosition = result.getCenter();
		double theta = Math.toRadians(result.getPositionAngle());
		AstroObj[] targets = result.getAstroObjects();

		double deadSpace = args.getDitherSpace() + OVERLAP / 2;
		double slitWidth = args.getSlitWidth();
		int ii=0;


		/* This part of the program simply takes the data from AstroObj array and
		 * maps it to a slit configuration. The empty bars are filled
		 * by expanding original "singles" into "doubles" or "triples" when
		 * possible and by expanding original "doubles" (which were created to
		 * include a high-priority object in an overlap region) into "triples"
		 * when possible. After this first pass expansion is complete, any
		 * remaining empty slits are filled by expanding the nearest slit with
		 * the higher-priority object. The slit expansion is conducted so as to 
		 * give more vertical space to the objects that are more likely to need 
		 * it and then to objects with higher priority. */

		//. for the CSU bars are numbered from top-down (1 is at top, 46 is at bottom)
		//. however, targets that are returned from mascgen are in reverse order, i.e.
		//. the first element in AstroObj is the object at the bottom, and then they go 
		//. up.  also, for AstroObj, the RR, and OR are reversed, so row RR is really
		//. 46-RR in CSU numbering.
		//. 
		//. IT IS VERY IMPORTANT THAT THE FOLLOWING IS TRUE OF THE AstroObj array:
		//. 
		//. 1) every object has a RR or OR value, but not both, indicating which row(s) the object is in.
		//.    if an object has an OR value of n, no object should have an RR or OR value of n-1.
		//. 2) the CSU (ObjX,Y) and WCS (WcsX,Y) coordinates of the objects are already properly calculated
		//. 
		//. all slit arrays created here will have that reverse order (lower index => higher slit number) 
		//. 
		//. the mechanical slit array is made first, but so we have to make sure
		//. the slit positions are assigned correctly.
		//. 

		// First, make a blank Slit Array.
		MechanicalSlit[] slitArray = new MechanicalSlit[CSU_NUMBER_OF_BAR_PAIRS];
		for (ii = 0; ii < slitArray.length; ii++) {
			//. reverse slit numbering
			slitArray[ii] = new MechanicalSlit(CSU_NUMBER_OF_BAR_PAIRS-ii);
		}


		//. next, go through astro objects, and assign them to their rows
		double xOld, yOld;
		double objX, objY;
		double ditherSpace = args.getDitherSpace();
		double maxY, minY;
		int minRow, maxRow;
		System.out.println("break");
		for (ii = 0; ii < targets.length; ii++) {
			AstroObj obj = targets[ii];
			
			//. if coord wrap, translate ra's to 11 and 12
			//. note result center should always maintain this transformation
			//. in its wcs and objX/Y values
			if (result.isCoordWrap()) {
				MascgenTransforms.applyRaCoordWrap(obj);
			}
			
			MascgenTransforms.astroObjRaDecToXY(obj, centerPosition);

			if (result.isCoordWrap()) {
				MascgenTransforms.fixRaCoordWrap(obj);
			}

			// Transform the entire astroObjArray into the CSU plane by subtracting
			// the center coordinate from each AstroObj's xCoordinate and 
			// yCoordinate and putting these into the ObjX and ObjY.
			xOld = obj.getWcsX() - centerPosition.getXCoordinate();
			yOld = obj.getWcsY() - centerPosition.getYCoordinate();

			// Rotate the objects in the CSU plane by the Position Angle.
			/* Objects were read in with coordinate system origin at center of 
			 *  CSU field. The optimize method runs with the coordinate system 
			 *  origin in the lower left. So, simply add CSU_WIDTH / 2 to the x  
			 *  position and CSU_HEIGHT / 2 to the y position of each object. 
			 */
			objX = (xOld * Math.cos(theta) - yOld * Math.sin(theta));
			objY = (xOld * Math.sin(theta) + yOld * Math.cos(theta));

			/* Crop out all AstroObjs in the astroObjArray that 
			 * 
			 * a) lie outside the  focal plane circle, defined by CSU_FP_RADIUS 
			 *    centered at the origin (CSU_WDITH / 2, CSU_HEIGHT / 2). 
			 * b) have x coordinate positions outside of the legal range. 
			 * c) have x coordinate positions outside of the CSU Plane. 
			 */
			//. need to check object stays in slit during dither
			//.
			//. coordinate system origin is at center, and goes positive to the left, and up
			//. with slits tilted 4 degrees counter-clockwise
			maxY = objY + ditherSpace * Math.cos(MosfireParameters.CSU_SLIT_TILT_ANGLE_RADIANS);
			minY = objY - ditherSpace * Math.cos(MosfireParameters.CSU_SLIT_TILT_ANGLE_RADIANS);
			
			obj.setObjX(objX);
			obj.setObjY(objY);

			//. determine what rows the object occupies during full dither
			minRow = (int)Math.floor((minY + CSU_HEIGHT / 2. - MosfireParameters.OVERLAP)/ MosfireParameters.CSU_ROW_HEIGHT);
			maxRow = (int)Math.floor((maxY + CSU_HEIGHT / 2. + MosfireParameters.OVERLAP)/ MosfireParameters.CSU_ROW_HEIGHT);
			obj.setMinRow(minRow);
			obj.setMaxRow(maxRow);

			//System.out.println("target: "+targets[ii].getObjName()+", ["+targets[ii].getMinRow()+","+targets[ii].getMaxRow()+"]");
			for (int jj=targets[ii].getMinRow(); jj<=targets[ii].getMaxRow(); jj++) {
				slitArray[jj].setTarget(targets[ii]);
				slitArray[jj].setSlitWidth(slitWidth);
				slitArray[jj].setSlitRows(targets[ii].getMaxRow() - targets[ii].getMinRow() + 1);
			}
		}
		if (reassignUnusedSlits) {

			// Extend slits in length to expand singles into double, doubles into
			// triples, etc.
			// First expand slit 1 into slit 2 and slit 44 into slit 43 if the 
			// first slits are originally singles and if 44 and 43 are unoccupied
			// and if slit 1's object has higher priority than slit 3's (and if 
			// slit 44's object has higher priority than slit 42's).
			// Then, lengthen occupied slits 2 and 45 into 1 and 46, respectively, 
			// if slits 1 and 46 are unoccupied. There is no need to compare 
			// priorities since there can be no objects in slits 0 or 47 (there are
			// no such slits).
			if ((slitArray[1].getSlitRows() == 1) &&
					(slitArray[2].getSlitRows() == -1) &&
					(slitArray[1].getTarget().getObjPriority() > 
					slitArray[3].getTarget().getObjPriority())) {
				slitArray[2] = slitArray[1].clone();
				slitArray[2].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS-2);
			}
			if ((slitArray[44].getSlitRows() == 1) &&
					(slitArray[43].getSlitRows() == -1) &&
					(slitArray[44].getTarget().getObjPriority() > 
					slitArray[42].getTarget().getObjPriority())) {
				slitArray[43] = slitArray[44].clone();
				slitArray[43].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS-43);
			}
			if ((slitArray[1].getSlitRows() == 1)
					&& (slitArray[0].getSlitRows() == -1)) {
				slitArray[0] = slitArray[1].clone();
				slitArray[0].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS);
			}   
			if ((slitArray[44].getSlitRows() == 1) 
					&& (slitArray[45].getSlitRows() == -1)) {
				slitArray[45] = slitArray[44].clone();
				slitArray[45].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS-45);
			}

			// Then extend all "singles" into "doubles" (or "triples") when possible 
			// and so that, if there is a conflict, the slit which is extended is 
			// the one that contains the higher-priority object. Note that we at 
			// first ignore the first set of doubles that were created in order to 
			// surround objects in overlap regions. This makes sense since those 
			// original doubles are guaranteed to have their target objects near 
			// their vertical slit center (within the middle overlap region).
			for (ii = 0; ii < slitArray.length; ii++) {
				if (slitArray[ii].getSlitRows() == 1) {
					if (ii < 44) {
						if (slitArray[ii + 1].getSlitRows() == -1) {
							if ((slitArray[ii].getTarget().getObjPriority() > 
							slitArray[ii + 2].getTarget().getObjPriority()) || 
							(slitArray[ii + 2].getSlitRows() == 2)) {
								slitArray[ii+1] = slitArray[ii].clone();
								slitArray[ii+1].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS - (ii+1));
							}
						}
					}
					if (ii > 1) {
						if (slitArray[ii - 1].getSlitRows() == -1) {
							if ((slitArray[ii].getTarget().getObjPriority() > 
							slitArray[ii - 2].getTarget().getObjPriority()) || 
							(slitArray[ii - 2].getSlitRows() == 2)) {
								slitArray[ii-1] = slitArray[ii].clone();
								slitArray[ii-1].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS - (ii-1));
							}
						}
					}
				}
			}

			// Extend all slits to envelope blanks. After this, every row should be 
			// occupied by a slit.
			for (int jj = 0; jj < 46; jj++) {
				for (ii = 1; ii < slitArray.length - 1; ii++) {
					if (slitArray[ii].getSlitRows() == -1) {

						if ((slitArray[ii - 1].getSlitRows() > 0) &&
								(slitArray[ii - 1].getTarget().getObjPriority() > 
								slitArray[ii + 1].getTarget().getObjPriority())) {
							slitArray[ii] = slitArray[ii-1].clone();
							slitArray[ii].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS - ii);
						} else if ((slitArray[ii + 1].getSlitRows() > 0) &&
								(slitArray[ii + 1].getTarget().getObjPriority() > 
								slitArray[ii - 1].getTarget().getObjPriority())) {
							slitArray[ii] = slitArray[ii+1].clone();
							slitArray[ii].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS - ii);
						} else if ((slitArray[ii + 1].getTarget().getObjPriority() == 
							slitArray[ii - 1].getTarget().getObjPriority())) {
							if ((slitArray[ii + 1].getTarget().getObjY() - deadSpace - (ii + 1) * (SINGLE_SLIT_HEIGHT) - 2 * ii * deadSpace) < 
									(deadSpace + (ii + 1) * (SINGLE_SLIT_HEIGHT) + 2 * ii * deadSpace - slitArray[ii + 1].getTarget().getObjY())) {
								slitArray[ii] = slitArray[ii+1].clone();
								slitArray[ii].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS - ii);
							}
							else {
								slitArray[ii] = slitArray[ii-1].clone();
								slitArray[ii].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS - ii);
							}
						}
					}
				}
			}

			if ((slitArray[45].getSlitRows() == -1) && 
					(slitArray[44].getSlitRows() > 0)) {
				slitArray[45] = slitArray[44].clone();
				slitArray[45].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS - 45);
			}
			if ((slitArray[0].getSlitRows() == -1) && 
					(slitArray[1].getSlitRows() > 0)) {
				slitArray[0] = slitArray[1].clone();
				slitArray[0].setSlitNumber(CSU_NUMBER_OF_BAR_PAIRS);
			}

			// Correct the slit rows values for each slit.
			String tmpObjName;
			int multiple = 0;
			for (ii = 0; ii < slitArray.length; ii++) {
				tmpObjName = slitArray[ii].getTarget().getObjName();
				multiple = 0;
				for (int jj = 0; jj < slitArray.length; jj++) {
					if (tmpObjName.equals(slitArray[jj].getTarget().getObjName())) {
						multiple++;
					}
				}
				slitArray[ii].setSlitRows(multiple);
			}
			//		printSlitPositions(slitArray);
		}
		Point2D.Double targetCsuCoords = new Point2D.Double();
		// Calculate the length, y-coordinate, and x-coordinate of each slit.
		for (ii = 0; ii < slitArray.length; ii++) {
			targetCsuCoords.x = slitArray[ii].getTarget().getObjX();
			targetCsuCoords.y = slitArray[ii].getTarget().getObjY();

			//. get position of slit in CSU Coordinates
			Point2D.Double slitPositionInCsuCoords = MascgenTransforms.getSlitPositionInCsuCoords(ii, 1, targetCsuCoords);

			//. in CSU coords, positions increase from right to left
			//. we want the same for center position (2011/03/16)
			slitArray[ii].setCenterPosition(slitPositionInCsuCoords.x);
		}
		//. This completes the info needed for the mechanical slit list.
		//		printSlitPositions(slitArray);

		//. reformat arrays as Lists and set in slit config
		//. sort mechanical slit list
		ArrayList<MechanicalSlit> tempList = new ArrayList<MechanicalSlit>(Arrays.asList(slitArray));
		
		Collections.sort(tempList, slitPositionSorter);

		config.setMechanicalSlitList(tempList);
		config.setScienceSlitList(generateScienceSlitListFromMechanicalList(tempList, args, result));

		//. check if alignment stars are needed
		if (args.getMinimumAlignmentStars() > 0) {
			//. find best set of alignment star objects
			AstroObj[] stars = findBestAlignmentSet(result, args);
			ArrayList<MechanicalSlit> alignSlitList = new ArrayList<MechanicalSlit>();

			int row = 0;
			double centerDistance;
			Point2D.Double slitPositionInCsuCoords;
			for (AstroObj alignTarget : stars) {
				//. determine which slit the star is in
				//. check overlap?  -  no, stars must be in row regions
				row = alignTarget.getObjRR();

				//. create new slit and set target
				MechanicalSlit newSlit = new MechanicalSlit(MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS - row);
				newSlit.setTarget(alignTarget);

				targetCsuCoords.x = newSlit.getTarget().getObjX();
				targetCsuCoords.y = newSlit.getTarget().getObjY();

				//. calculate position of slit in CSU coordinates
				slitPositionInCsuCoords = MascgenTransforms.getSlitPositionInCsuCoords(row, 1, targetCsuCoords);

				//. center position of slit is X position, since star is centered
				//. and coordinate system of CSU goes right to left
				//. we want the same for center position (2011/03/16)
				newSlit.setCenterPosition(slitPositionInCsuCoords.x);
				//. slit width is standard with for stars
				newSlit.setSlitWidth(MosfireParameters.ALIGNMENT_BOX_SLIT_WIDTH);
				//. set center target distance  
				centerDistance = (targetCsuCoords.y - slitPositionInCsuCoords.y)/(Math.cos(CSU_SLIT_TILT_ANGLE_RADIANS));
				newSlit.setCenterDistance(centerDistance);
				alignTarget.setCenterDistance(centerDistance);
				//. add to list
				alignSlitList.add(newSlit);
			}

			//. set list in config
			config.setAlignSlitList(alignSlitList);
		}

		return config;
	}
	
	/**
	 * Generate science slit list from mechanical list.
	 *
	 * @param  mechanicalList MechanicalSlit ArrayList containing mechanical slits for configuration
	 * @param  args           MascgenArguments object specifying parameters for configuration
	 * @param  result         MascgenResult object with result from MASCGEN
	 * @return                ScienceSlit ArrayList of science slits for configuration
	 */
	private static ArrayList<ScienceSlit> generateScienceSlitListFromMechanicalList(ArrayList<MechanicalSlit> mechanicalList, MascgenArguments args, MascgenResult result) {
		//. now, we will create science slit list.
		//. at this point, the slit array has 46 elements. 
		//. the goal of the next part of the routine is to
		//. reduce them to have 1 slit for every object, and to calculate what the 
		//. slit length, ra/dec, and targetCenterDistance is.

		MechanicalSlit[] slitArray = mechanicalList.toArray(new MechanicalSlit[0]);
		
		//. figure out how many unique objects
		int ii = 0;
		int scienceSlitArrayLength = 0;
		while (ii < slitArray.length){
			if (slitArray[ii].getSlitRows() < 0) {
				ii++;
			} else {
				scienceSlitArrayLength++;
				ii+=slitArray[ii].getSlitRows();
			}
		}

		//. make an array for holding our ScienceSlits
		ArrayList<ScienceSlit> newScienceSlitArray = new ArrayList<ScienceSlit>();

		//. loop through mechanical slit list, and copy first instance of a slit with 
		//. a new object into the new science slit array.  remember, we'll construct this 
		//. array in reverse order as well.
		ii=0;
		int slitNumber=0;
		double centerDistance;
		Point2D.Double wcs;
		Point2D.Double slitPositionInCsuCoords;
		Point2D.Double targetCsuCoords = new Point2D.Double();
		while (ii < slitArray.length) {
			if (slitArray[ii].getSlitRows() < 0) {
				ii++;
			} else {
//				ScienceSlit tempScienceSlit = new ScienceSlit(scienceSlitArrayLength - slitNumber, slitArray[ii]);
				ScienceSlit tempScienceSlit = new ScienceSlit(slitNumber+1, slitArray[ii]);

				targetCsuCoords.x = tempScienceSlit.getTarget().getObjX();
				targetCsuCoords.y = tempScienceSlit.getTarget().getObjY();

				//. get position of center of slit in CSU coordinates
				slitPositionInCsuCoords = MascgenTransforms.getSlitPositionInCsuCoords(MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS - ii - tempScienceSlit.getSlitRows(), tempScienceSlit.getSlitRows(), targetCsuCoords);

				//System.out.println(tempScienceSlit.getSlitNumber()+"\tslitX:"+slitPositionInCsuCoords.x+"\tslitY="+slitPositionInCsuCoords.y);

				//. get WCS coordinates
				wcs = MascgenTransforms.getWcsFromCSUCoords(slitPositionInCsuCoords, result.getCenter(), result.getPositionAngle());

				//. get RA/Dec from WCS and set in sli
				RaDec slitRaDec = MascgenTransforms.getRaDecFromWcs(wcs, result.getCenter());
				
				if (result.isCoordWrap()) {
					int h = slitRaDec.getRaHour();
					h -= 12;
					if (h < 0) h+=24;
					slitRaDec.setRaHour(h);
				}
				
				tempScienceSlit.setSlitRaDec(slitRaDec);

				//. calculate center distance by subtracting target y from slit center y 
				centerDistance = (tempScienceSlit.getTarget().getObjY() - slitPositionInCsuCoords.y)/(Math.cos(CSU_SLIT_TILT_ANGLE_RADIANS));
				tempScienceSlit.setCenterDistance(centerDistance);
				tempScienceSlit.getTarget().setCenterDistance(centerDistance);
				tempScienceSlit.getTarget().setInValidSlit(Math.abs(centerDistance) <= (tempScienceSlit.getSlitLength()/2 - args.getDitherSpace()));

				newScienceSlitArray.add(tempScienceSlit);
				slitNumber++;
				ii+=slitArray[ii].getSlitRows();
			}
		}
		return newScienceSlitArray;
	}
	
	/**
	 * Align slit with neighbor, either above or below.
	 *
	 * @param  mechSlitNumber                 0-based index of mechanical slit list for row to adjust
	 * @param  alignWithAbove                 Whether to align with slit above (true) or below (false)
	 * @throws ArrayIndexOutOfBoundsException if <code>mechSlitNumber</code> is outside of bounds of mechanical slit list
	 */
	public void alignSlitWithNeighbor(int mechSlitNumber, boolean alignWithAbove) throws ArrayIndexOutOfBoundsException {
		//. mechSlitNumber is 0-based starting from top
		//. TODO: below won't work with longslit?  this function doesn't make sense for long slit anyway
		MechanicalSlit mechSlit = mechanicalSlitList.get(mechSlitNumber);
		
		//. get mech slit for neighbor
		MechanicalSlit neighborMechSlit;
		if (alignWithAbove) { 
			neighborMechSlit = mechanicalSlitList.get(mechSlitNumber-1);
		} else {
			neighborMechSlit = mechanicalSlitList.get(mechSlitNumber+1);
		}

		//. get original and neighbor targets
		AstroObj origTarget = mechSlit.getTarget();
		AstroObj newTarget = neighborMechSlit.getTarget();

		//. if they have the same target, they should already be aligned
		if (!origTarget.equals(newTarget)) {
			
			//. set neighbor target in this slit
			mechSlit.setTarget(newTarget);
			
			//. need to fix science slits
			//. the length (slit rows), center position, and coords must be fixed
			ScienceSlit targetScienceSlit = getScienceSlitWithAstroObj(newTarget);
			ScienceSlit origScienceSlit = getScienceSlitWithAstroObj(origTarget);
			
			if ((targetScienceSlit == null) || (origScienceSlit == null)) {
				throw new ArrayIndexOutOfBoundsException("Error finding science slits.");
			}
			setStatus(STATUS_MODIFIED);

			//. determine new rows
			int newTargetSlitRows = targetScienceSlit.getSlitRows()+1;
			int newOrigSlitRows = origScienceSlit.getSlitRows()-1;
			neighborMechSlit.setSlitRows(newTargetSlitRows);
			mechSlit.setSlitRows(newTargetSlitRows);
			
			Point2D.Double targetCsuCoords = new Point2D.Double();
			targetCsuCoords.x = newTarget.getObjX();
			targetCsuCoords.y = newTarget.getObjY();

			//. get position of slit in CSU Coordinates
			Point2D.Double slitPositionInCsuCoords = MascgenTransforms.getSlitPositionInCsuCoords(CSU_NUMBER_OF_BAR_PAIRS - mechSlitNumber - 1, 1, targetCsuCoords);

			//. in CSU coords, positions increase from right to left
			//. we want the same for center position (2011/03/16)
			mechSlit.setCenterPosition(slitPositionInCsuCoords.x);

			//. now redo science slits
			double centerDistance;
			Point2D.Double wcs;
			int targetStartRow, origStartRow;
			if (alignWithAbove) {
				targetStartRow = CSU_NUMBER_OF_BAR_PAIRS - mechSlitNumber - 1;
				origStartRow = CSU_NUMBER_OF_BAR_PAIRS - mechSlitNumber - newOrigSlitRows - 1;
			} else {
				targetStartRow = CSU_NUMBER_OF_BAR_PAIRS - mechSlitNumber - newTargetSlitRows;
				origStartRow = CSU_NUMBER_OF_BAR_PAIRS - mechSlitNumber;
			}
			
			//. do new longer slit first
			targetScienceSlit.setSlitLength(newTargetSlitRows * MosfireParameters.CSU_ROW_HEIGHT - MosfireParameters.OVERLAP);

			//. get position of center of slit in CSU coordinates
			//. startRow is row of new slit, since it is bottom
			slitPositionInCsuCoords = MascgenTransforms.getSlitPositionInCsuCoords(targetStartRow, newTargetSlitRows, targetCsuCoords);

			//. get WCS coordinates
			wcs = MascgenTransforms.getWcsFromCSUCoords(slitPositionInCsuCoords, mascgenResult.getCenter(), mascgenResult.getPositionAngle());

			//. get RA/Dec from WCS and set in slit
			targetScienceSlit.setSlitRaDec(MascgenTransforms.getRaDecFromWcs(wcs, mascgenResult.getCenter()));

			//. calculate center distance by subtracting target y from slit center y 
			centerDistance = (newTarget.getObjY() - slitPositionInCsuCoords.y)/(Math.cos(CSU_SLIT_TILT_ANGLE_RADIANS));
			targetScienceSlit.setCenterDistance(centerDistance);
			newTarget.setCenterDistance(centerDistance);

			targetScienceSlit.getTarget().setInValidSlit(Math.abs(centerDistance) < (targetScienceSlit.getSlitLength()/2 - mascgenArgs.getDitherSpace()));

			//. now do old slit.  if its rows isn't positive, it's gone.  remove it.
			if (newOrigSlitRows > 0) {
				targetCsuCoords.x = origTarget.getObjX();
				targetCsuCoords.y = origTarget.getObjY();

				
				//. do new longer slit first
				origScienceSlit.setSlitLength(newOrigSlitRows * MosfireParameters.CSU_ROW_HEIGHT - MosfireParameters.OVERLAP);

				//. get position of center of slit in CSU coordinates
				slitPositionInCsuCoords = MascgenTransforms.getSlitPositionInCsuCoords(origStartRow, newOrigSlitRows, targetCsuCoords);

				//. get WCS coordinates
				wcs = MascgenTransforms.getWcsFromCSUCoords(slitPositionInCsuCoords, mascgenResult.getCenter(), mascgenResult.getPositionAngle());

				//. get RA/Dec from WCS and set in slit
				origScienceSlit.setSlitRaDec(MascgenTransforms.getRaDecFromWcs(wcs, mascgenResult.getCenter()));

				//. calculate center distance by subtracting target y from slit center y 
				centerDistance = (origTarget.getObjY() - slitPositionInCsuCoords.y)/(Math.cos(CSU_SLIT_TILT_ANGLE_RADIANS));
				origScienceSlit.setCenterDistance(centerDistance);
				origTarget.setCenterDistance(centerDistance);
				
				origScienceSlit.getTarget().setInValidSlit(Math.abs(centerDistance) < (origScienceSlit.getSlitLength()/2 - mascgenArgs.getDitherSpace()));
			} else {
				//. remove target from list
				removeScienceSlit(origScienceSlit);
			}
			
			//. if we lost an object, priority needs to be fixed
			updatePriority();
		}
	}

	/**
	 * Removes a science slit from list.
	 *
	 * @param slit ScienceSlit to remove
	 */
	private void removeScienceSlit(ScienceSlit slit) {
		int slitIndex = slit.getSlitNumber();
		for (ScienceSlit current : scienceSlitList) {
			if (current.getSlitNumber() > slitIndex) {
				current.setSlitNumber(current.getSlitNumber() - 1);
			}
		}
		scienceSlitList.remove(slit);		
	}

	/**
	 * Move slit plus all neighboring slits onto specified target so that new science slit is valid.
	 *
	 * @param  mechSlitNumber 0-based index of slit from mechanical slit list
	 * @param  newTarget      AstroObj target to put slit on
	 * @return                False if a valid slit cannot be put on target.  True otherwise.
	 */
	public boolean moveSlitOntoTarget(int mechSlitNumber, AstroObj newTarget) {
		//. move current slit plus all neighboring slits so that new science slit
		//. with new target is valid.
		
		//. get original slit
		//. mechSlitNumber is 0-based starting from top
		//. TODO: below won't work with longslit?  this function doesn't make sense for long slit anyway
		MechanicalSlit mechSlit = mechanicalSlitList.get(mechSlitNumber);

		//. make sure X and Y are updated for obj
		double theta = Math.toRadians(mascgenResult.getPositionAngle());
		if (mascgenResult.isCoordWrap()) {
			MascgenTransforms.applyRaCoordWrap(newTarget);
		}
		MascgenTransforms.astroObjRaDecToXY(newTarget, mascgenResult.getCenter());
		if (mascgenResult.isCoordWrap()) {
			MascgenTransforms.fixRaCoordWrap(newTarget);
		}
		double xOld = newTarget.getWcsX() - mascgenResult.getCenter().getXCoordinate();
		double yOld = newTarget.getWcsY() - mascgenResult.getCenter().getYCoordinate();
		newTarget.setObjX(xOld * Math.cos(theta) - yOld * Math.sin(theta));
		newTarget.setObjY(xOld * Math.sin(theta) + yOld * Math.cos(theta));
		newTarget.updateDitherRows(mascgenArgs.getDitherSpace());

		logger.debug("min row = "+newTarget.getMinRow()+", max row = "+newTarget.getMaxRow());

		//. get original target
		AstroObj origTarget = mechSlit.getTarget();
		ScienceSlit origScienceSlit = getScienceSlitWithAstroObj(origTarget);

		if (origScienceSlit == null) {
			//. TODO improve error handling.
			throw new ArrayIndexOutOfBoundsException("Error finding science slits.");
		}
		//. note: mechSlitNumber is zero based,
		//. but the rest of the rows here is 1 based.
		int origScienceSlitRows = origScienceSlit.getSlitRows();
		
		int origScienceSlitCenterRow = MascgenTransforms.getRowFromRaDec(origScienceSlit.getSlitRaDec(), mascgenResult.getCenter(), mascgenResult.getPositionAngle());
		int origStartRow;
		int origEndRow;
		if (origScienceSlitRows % 2 == 0) {
			origStartRow = origScienceSlitCenterRow - origScienceSlitRows/2 + 1;
			origEndRow = origScienceSlitCenterRow + origScienceSlitRows/2;
		} else {
			origStartRow = origScienceSlitCenterRow - (origScienceSlitRows-1)/2;
			origEndRow = origScienceSlitCenterRow + (origScienceSlitRows-1)/2;
		}
		//. need to prevent non-contiguous slits.  this can only happen
		//. if new slit is within old slit.  if this happens, move whole slit
		//. unless old target is still in a slit, and then keep that side, if so.

		//. determine what row orig target is in
		int origTargetRow = MascgenTransforms.getRowFromRaDec(origTarget.getRaDec(), mascgenResult.getCenter(), mascgenResult.getPositionAngle());

		int newTargetStartRow = CSU_NUMBER_OF_BAR_PAIRS - newTarget.getMaxRow();
		int newTargetEndRow = CSU_NUMBER_OF_BAR_PAIRS - newTarget.getMinRow();
		int newSlitStartRow = newTargetStartRow;
		int newSlitEndRow = newTargetEndRow;
		if ((origStartRow < newTargetStartRow) && (origEndRow > newTargetEndRow)) {
			if (origTargetRow < newTargetStartRow) {
				newSlitEndRow = origEndRow;
			} else if (origTargetRow > newTargetEndRow) {
				newSlitStartRow = origStartRow;
			} else {
				newSlitStartRow = origStartRow;
				newSlitEndRow = origEndRow;
			}
		}
		
		//. if new slit is beyond limits, return false
		if ((newSlitStartRow < 1) || (newSlitEndRow > 46)) {
			return false;
		}


		Point2D.Double slitPositionInCsuCoords;
		Point2D.Double targetCsuCoords = new Point2D.Double(newTarget.getObjX(), newTarget.getObjY());

		//. store old center positions in case there is a problem
		double[] oldCenterPositions = new double[newSlitEndRow - newSlitStartRow + 1];
		
		//. now set mechanical slit center positions for new slits
		//. assign new target to all rows it will dither into, taking them away from
		//. slits there were previously part of.  
		//. note: rows as stored by AstroObj's are in reverse order (0-45 from top)
		for (int ii = newSlitStartRow; ii < newSlitEndRow+1; ii++) {
			mechSlit = mechanicalSlitList.get(ii - 1);

			oldCenterPositions[ii - newSlitStartRow] = mechSlit.getCenterPosition();
			
			//. get position of slit in CSU Coordinates
			slitPositionInCsuCoords = MascgenTransforms.getSlitPositionInCsuCoords(CSU_NUMBER_OF_BAR_PAIRS - ii, 1, targetCsuCoords);
			
			//. in CSU coords, positions increase from right to left
			//. we want the same for center position (2011/03/16)
			mechSlit.setCenterPosition(slitPositionInCsuCoords.x);

			if ((mechSlit.getLeftBarPositionInMM()  > MosfireParameters.CSU_MAXIMUM_BAR_POSITION_MM) || (mechSlit.getRightBarPositionInMM() < MosfireParameters.CSU_MINIMUM_BAR_POSITION_MM)) {
				//. fix center positions
				for (int jj=newSlitStartRow; jj < ii+1; jj++ ) {
					mechSlit = mechanicalSlitList.get(jj - 1);
					mechSlit.setCenterPosition(oldCenterPositions[jj - newSlitStartRow]);
					mechSlit.setTarget(origTarget);
				}
				//. bail
				return false;
			}
			
			mechSlit.setTarget(newTarget);
		}
		setStatus(STATUS_MODIFIED);

		updateMechanicalListTargets();
		//. update science slit list
		setScienceSlitList(generateScienceSlitListFromMechanicalList(mechanicalSlitList, mascgenArgs, mascgenResult));
		updatePriority();
		
		return true;
	}
	
	/**
	 * Write slit configuration to disk as MOSFIRE Slit Configuration with unknown version.
	 *
	 * @throws JDOMException if error creating MSC.
	 * @throws IOException   on error writing MSC to disk.
	 */
	public void writeSlitConfiguration() throws JDOMException, IOException {
		writeSlitConfiguration("unknown");
	}

	/**
	 * Write slit configuration to disk as MOSFIRE Slit Configuration with specified version.
	 *
	 * @param  version       String giving version of MSC
	 * @throws JDOMException if error creating MSC.
	 * @throws IOException   on error writing MSC to disk.
	 */
	public void writeSlitConfiguration(String version) throws JDOMException, IOException {
		writeSlitConfiguration(new File(mascgenArgs.getFullPathOutputMSC()), version);
	}

	/**
	 * Write slit configuration to disk as MOSFIRE Slit Configuration to specified file with specified version.
	 *
	 * @param  file          File object giving target of MSC
	 * @param  version       String giving version of MSC
	 * @throws JDOMException if error creating MSC.
	 * @throws IOException   on error writing MSC to disk.
	 */
	public void writeSlitConfiguration(File file, String version) throws JDOMException, IOException {
		Element newElement;
		RaDec currentRaDec;

		//. root element 
		Element root = new Element(XML_ROOT);
		root.setAttribute(XML_ATTRIBUTE_MSC_VERSION, version);

		newElement = new Element(XML_ELEMENT_MASK_DESCRIPTION);

		newElement.setAttribute(XML_ATTRIBUTE_MASK_NAME, maskName);
		newElement.setAttribute(XML_ATTRIBUTE_TOTAL_PRIORITY, twoDigitFormatter.format(mascgenResult.getTotalPriority()));
		currentRaDec = mascgenResult.getCenter(); 
		newElement.setAttribute(XML_ATTRIBUTE_CENTER_RAH, twoDigitWholeNumberFormatter.format(currentRaDec.getRaHour()));
		newElement.setAttribute(XML_ATTRIBUTE_CENTER_RAM, twoDigitWholeNumberFormatter.format(currentRaDec.getRaMin()));
		newElement.setAttribute(XML_ATTRIBUTE_CENTER_RAS, degreeSecondFormatter.format(currentRaDec.getRaSec()));
		newElement.setAttribute(XML_ATTRIBUTE_CENTER_DECD, twoDigitWholeNumberFormatter.format(currentRaDec.getDecDeg()));
		newElement.setAttribute(XML_ATTRIBUTE_CENTER_DECM, twoDigitWholeNumberFormatter.format(currentRaDec.getDecMin()));
		newElement.setAttribute(XML_ATTRIBUTE_CENTER_DECS, degreeSecondFormatter.format(currentRaDec.getDecSec()));
		newElement.setAttribute(XML_ATTRIBUTE_MASK_PA, twoDigitFormatter.format(mascgenResult.getPositionAngle()));

		root.addContent(newElement);

		Element mechSlitConfig = new Element(XML_ELEMENT_MECHANICAL_SLIT_CONFIG);

		if (mechanicalSlitList != null) {
			ArrayList<MechanicalSlit> tempList = new ArrayList<MechanicalSlit>(mechanicalSlitList);
			
			Collections.sort(tempList, slitPositionSorter);

			//. write slit position elements
			for (MechanicalSlit pos : tempList) {
				newElement = new Element(XML_ELEMENT_MECHANICAL_SLIT);
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_NUMBER, Integer.toString(pos.getSlitNumber()));
				newElement.setAttribute(XML_ATTRIBUTE_LEFT_BAR_NUMBER, Integer.toString(pos.getLeftBarNumber()));
				newElement.setAttribute(XML_ATTRIBUTE_RIGHT_BAR_NUMBER, Integer.toString(pos.getRightBarNumber()));
				newElement.setAttribute(XML_ATTRIBUTE_LEFT_BAR_POSITION_MM, threeDigitFormatter.format(pos.getLeftBarPositionInMM()));
				newElement.setAttribute(XML_ATTRIBUTE_RIGHT_BAR_POSITION_MM, threeDigitFormatter.format(pos.getRightBarPositionInMM()));
				newElement.setAttribute(XML_ATTRIBUTE_CENTER_POSITION, threeDigitFormatter.format(pos.getCenterPosition()));
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_WIDTH, threeDigitFormatter.format(pos.getSlitWidth()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET, pos.getTargetName());

				mechSlitConfig.addContent(newElement);	    	
			}
		}

		root.addContent(mechSlitConfig);

		Element scienceSlitConfig = new Element(XML_ELEMENT_SCIENCE_SLIT_CONFIG);

		if (scienceSlitConfig != null) {
			ArrayList<ScienceSlit> tempList = new ArrayList<ScienceSlit>(scienceSlitList);
			
			Collections.sort(tempList, slitPositionSorter);

			
			for (ScienceSlit slit : tempList) {
				newElement = new Element(XML_ELEMENT_SCIENCE_SLIT);
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_NUMBER, Integer.toString(slit.getSlitNumber()));
				currentRaDec = slit.getSlitRaDec();
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_RAH, twoDigitWholeNumberFormatter.format(currentRaDec.getRaHour()));
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_RAM, twoDigitWholeNumberFormatter.format(currentRaDec.getRaMin()));
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_RAS, degreeSecondFormatter.format(currentRaDec.getRaSec()));
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_DECD, twoDigitWholeNumberFormatter.format(currentRaDec.getDecDeg()));
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_DECM, twoDigitWholeNumberFormatter.format(currentRaDec.getDecMin()));
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_DECS, degreeSecondFormatter.format(currentRaDec.getDecSec()));
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_WIDTH, twoDigitFormatter.format(slit.getSlitWidth()));
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_LENGTH, twoDigitFormatter.format(slit.getSlitLength()));

				AstroObj target = slit.getTarget();
				newElement.setAttribute(XML_ATTRIBUTE_TARGET, target.getObjName());
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_PRIORITY, twoDigitFormatter.format(target.getObjPriority()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_MAGNITUDE, twoDigitFormatter.format(target.getObjMag()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_CENTER_DISTANCE, twoDigitFormatter.format(slit.getCenterDistance()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_RAH, twoDigitWholeNumberFormatter.format(target.getRaHour()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_RAM, twoDigitWholeNumberFormatter.format(target.getRaMin()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_RAS, degreeSecondFormatter.format(target.getRaSec()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_DECD, twoDigitWholeNumberFormatter.format(target.getDecDeg()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_DECM, twoDigitWholeNumberFormatter.format(target.getDecMin()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_DECS, degreeSecondFormatter.format(target.getDecSec()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_EPOCH, twoDigitFormatter.format(target.getEpoch()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_EQUINOX, twoDigitFormatter.format(target.getEquinox()));

				scienceSlitConfig.addContent(newElement);
			}
		}
		root.addContent(scienceSlitConfig);

		Element alignment = new Element(XML_ELEMENT_ALIGNMENT);

		if (alignSlitList != null) {
			ArrayList<MechanicalSlit> tempList = new ArrayList<MechanicalSlit>(alignSlitList);
			
			Collections.sort(tempList, slitPositionSorter);

			
			for (MechanicalSlit slit : tempList) {
				newElement = new Element(XML_ELEMENT_ALIGNMENT_SLIT);
				newElement.setAttribute(XML_ATTRIBUTE_MECH_SLIT_NUMBER, Integer.toString(slit.getSlitNumber()));
				newElement.setAttribute(XML_ATTRIBUTE_LEFT_BAR_NUMBER, Integer.toString(slit.getLeftBarNumber()));
				newElement.setAttribute(XML_ATTRIBUTE_RIGHT_BAR_NUMBER, Integer.toString(slit.getRightBarNumber()));
				newElement.setAttribute(XML_ATTRIBUTE_LEFT_BAR_POSITION_MM, threeDigitFormatter.format(slit.getLeftBarPositionInMM()));
				newElement.setAttribute(XML_ATTRIBUTE_RIGHT_BAR_POSITION_MM, threeDigitFormatter.format(slit.getRightBarPositionInMM()));
				newElement.setAttribute(XML_ATTRIBUTE_CENTER_POSITION, threeDigitFormatter.format(slit.getCenterPosition()));
				newElement.setAttribute(XML_ATTRIBUTE_SLIT_WIDTH, threeDigitFormatter.format(slit.getSlitWidth()));

				AstroObj target = slit.getTarget();
				newElement.setAttribute(XML_ATTRIBUTE_TARGET, target.getObjName());
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_PRIORITY, twoDigitFormatter.format(target.getObjPriority()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_MAGNITUDE, twoDigitFormatter.format(target.getObjMag()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_CENTER_DISTANCE, twoDigitFormatter.format(slit.getCenterDistance()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_RAH, twoDigitWholeNumberFormatter.format(target.getRaHour()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_RAM, twoDigitWholeNumberFormatter.format(target.getRaMin()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_RAS, degreeSecondFormatter.format(target.getRaSec()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_DECD, twoDigitWholeNumberFormatter.format(target.getDecDeg()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_DECM, twoDigitWholeNumberFormatter.format(target.getDecMin()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_DECS, degreeSecondFormatter.format(target.getDecSec()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_EPOCH, twoDigitFormatter.format(target.getEpoch()));
				newElement.setAttribute(XML_ATTRIBUTE_TARGET_EQUINOX, twoDigitFormatter.format(target.getEquinox()));

				alignment.addContent(newElement);
			}
		}

		root.addContent(alignment);

		root.addContent(MascgenArguments.getMascgenArgumentsRootElement(mascgenArgs));

		Document doc = new Document();
		//. add xslt tranformation tag
		//. have to create new one each time, so that the instruction's parent object is reset.
		//. a ClassCastException in thrown otherwise (although doesn't appear to be fatal).
		doc.addContent(new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\""+MosfireParameters.DEFAULT_MSC_XSLT_URL+"\""));

		doc.setRootElement(root);
		
		outputter.output(doc, new java.io.FileOutputStream(file));

		originalFilename = file.getAbsolutePath();
		status = STATUS_SAVED;
	}

	/**
	 * Write slit configuration in HTML format to file specified in <code>MascgenArguments</code>.
	 *
   * @throws TransformerException   on error constructing HTML
   * @throws MalformedURLException  on error getting XSLT transformation URL
   */
	public void writeSlitConfigurationHTML() throws TransformerException, MalformedURLException {
		writeSlitConfigurationHTML(new File(mascgenArgs.getFullPathOutputMSC()));
	}

	/**
	 * Write slit configuration in HTML format to specified file.
	 *
	 * @param  mscFile                File object pointing to file to write to
   * @throws TransformerException   on error constructing HTML
   * @throws MalformedURLException  on error getting XSLT transformation URL
   */
	public void writeSlitConfigurationHTML(File mscFile) throws TransformerException, MalformedURLException {
		TransformerFactory tFactory = TransformerFactory.newInstance();
		URL xsltURL = new URL(MosfireParameters.DEFAULT_MSC_XSLT_URL);
		SecurityManager manager = System.getSecurityManager();
		boolean useURL = true;
		try {
			manager.checkConnect(MosfireParameters.DEFAULT_MSC_XSLT_HOST, 80);
		} catch (SecurityException ex) {
			System.out.println("Permission denied: Cannot connect to "+MosfireParameters.DEFAULT_MSC_XSLT_URL);
			useURL = false;
		}
		if (useURL) {
			Transformer transformer = tFactory.newTransformer(new StreamSource(xsltURL.toExternalForm()));
			transformer.transform(new StreamSource(mscFile), new StreamResult(mscFile.getAbsolutePath()+".html"));
		} else {
			if (MosfireParameters.DEFAULT_MSC_XSLT_FILE.exists()) {
				Transformer transformer = tFactory.newTransformer(new StreamSource(MosfireParameters.DEFAULT_MSC_XSLT_FILE));
				transformer.transform(new StreamSource(mscFile), new StreamResult(mscFile.getAbsolutePath()+".html"));
			} else {
				throw new TransformerException("XSLT URL <"+MosfireParameters.DEFAULT_MSC_XSLT_URL+"> cannot be acecssed, and file <"+MosfireParameters.DEFAULT_MSC_XSLT_FILE+"> does not exist.");
			}
		}
	}
	
	/**
	 * Write MASCGEN parameters to file specified in <code>MascgenArguments</code>.
	 *
	 * @throws JDOMException on error constructing XML
	 * @throws IOException   on error writing file
	 */
	public void writeMascgenParams() throws JDOMException, IOException {
		writeMascgenParams(mascgenArgs.getFullPathOutputMascgenParams());
	}
	
	/**
	 * Write MASCGEN parameters to specified filename.
	 *
	 * @param  filename      String path of file to write to
	 * @throws JDOMException on error constructing XML
	 * @throws IOException   on error writing file
	 */
	public void writeMascgenParams(String filename) throws JDOMException, IOException {
		mascgenArgs.writeMascgenParamFile(new File(filename));
	}

	/**
	 * Write original target list to file specified in <code>MascgenArguments</code>.
	 *
	 * @throws FileNotFoundException if File cannot be created
	 */
	public void writeOrigCoordsFile() throws FileNotFoundException{
		writeOrigCoordsFile(mascgenArgs.getFullPathOutputAllTargets());
	}
	
	
	/**
	 * Write original target list to disk.
	 *
	 * @param fileName               String containing filename for target list
	 * @throws FileNotFoundException if File cannot be created from <code>fileName</code>
	 */
	public void writeOrigCoordsFile(String fileName) throws FileNotFoundException{
		FileOutputStream out = new FileOutputStream(fileName);
		PrintStream p = new PrintStream(out);
		
		for (AstroObj target : originalTargetList) {
			p.printf("%s  %7.2f  %5.2f  %02.0f  %02.0f  %06.3f  % 03.0f  %02.0f  %05.2f  %6.1f  %6.1f  %3.1f  %3.1f\n", 
					target.getObjName(), target.getObjPriority(), 
					target.getObjMag(), target.getRaHour(), target.getRaMin(), 
					target.getRaSec(), target.getDecDeg(), target.getDecMin(), 
					target.getDecSec(), target.getEpoch(), target.getEquinox(), 0.0, 0.0);			
		}
		p.close();
	}	
	
	/**
	 * Write target list of objects in mask to file specified in <code>MascgenArguments</code>.
	 *
	 * @throws FileNotFoundException if File cannot be created
	 */
	public void writeCoordsFile() throws FileNotFoundException{
		writeCoordsFile(mascgenArgs.getFullPathOutputMaskTargets());
	}

	/**
	 * Write target list of objects in mask to disk.
	 *
	 * @param fileName               String containing filename for target list
	 * @throws FileNotFoundException if File cannot be created from <code>fileName</code>
	 */
	public void writeCoordsFile(String fileName) throws FileNotFoundException{
		FileOutputStream out = new FileOutputStream(fileName);
		PrintStream p = new PrintStream(out);

		for (ScienceSlit thisSlit: scienceSlitList){ 
			AstroObj target = thisSlit.getTarget();

			p.printf("%s  %7.2f  %5.2f  %02.0f  %02.0f  %06.3f  % 03.0f  %02.0f  %05.2f  %6.1f  %6.1f  %3.1f  %3.1f\n", 
					target.getObjName(), target.getObjPriority(), 
					target.getObjMag(), target.getRaHour(), target.getRaMin(), 
					target.getRaSec(), target.getDecDeg(), target.getDecMin(), 
					target.getDecSec(), target.getEpoch(), target.getEquinox(), 0.0, 0.0);

		}
		for (MechanicalSlit thisSlit: alignSlitList){ 
			AstroObj target = thisSlit.getTarget();

			p.printf("%s  %7.2f  %5.2f  %02.0f  %02.0f  %06.3f  % 03.0f  %02.0f  %05.2f  %6.1f  %6.1f  %3.1f  %3.1f\n", 
					target.getObjName(), target.getObjPriority(), 
					target.getObjMag(), target.getRaHour(), target.getRaMin(), 
					target.getRaSec(), target.getDecDeg(), target.getDecMin(), 
					target.getDecSec(), target.getEpoch(), target.getEquinox(), 0.0, 0.0);

		}
		p.close();
	}

	/**
	 * Write science slit list to disk to file specified in <code>MascgenArguments</code>.
	 *
	 * @throws FileNotFoundException if File cannot be created
	 */
	public void writeOutSlitList() throws FileNotFoundException {
		writeOutSlitList(mascgenArgs.getFullPathOutputSlitList());
	}

	/**
	 * Write science slit list to disk to specified file.
	 *
	 * @param  outputSlitFile        String containing filename for target list
	 * @throws FileNotFoundException if File cannot be created from <code>outputSlitFile</code>
	 */
	public void writeOutSlitList(String outputSlitFile) throws FileNotFoundException {
		twoDigitFormatter.setRoundingMode(RoundingMode.HALF_UP);
		
		FileOutputStream out = new FileOutputStream(outputSlitFile);
		PrintStream p = new PrintStream(out);

		ArrayList<ScienceSlit> tempList = new ArrayList<ScienceSlit>(scienceSlitList);
		
		Collections.sort(tempList, slitPositionSorter);
		
		for (ScienceSlit slit : tempList) {
			RaDec slitRaDec = slit.getSlitRaDec();
			AstroObj target = slit.getTarget();
			p.println(slit.getSlitNumber() + "\t" + 
					twoDigitWholeNumberFormatter.format(slitRaDec.getRaHour()) + "\t" + 
					twoDigitWholeNumberFormatter.format(slitRaDec.getRaMin()) + "\t" + 
					degreeSecondFormatter.format(slit.getSlitRaDec().getRaSec()) + "\t" + 
					twoDigitWholeNumberFormatter.format(slitRaDec.getDecDeg()) + "\t" + 
					twoDigitWholeNumberFormatter.format(slitRaDec.getDecMin()) + "\t" + 
					degreeSecondFormatter.format(slitRaDec.getDecSec()) + "\t" + 
					twoDigitFormatter.format(slit.getSlitWidth()) + "\t" + 
					twoDigitFormatter.format(slit.getSlitLength()) + "\t" + 
					target.getObjName() + "\t" + 
					twoDigitFormatter.format(target.getObjPriority()) + "\t" + 
					twoDigitFormatter.format(slit.getCenterDistance()) + "\t" +
					twoDigitWholeNumberFormatter.format(target.getRaHour()) + "\t" + 
					twoDigitWholeNumberFormatter.format(target.getRaMin()) + "\t" + 
					degreeSecondFormatter.format(target.getRaSec()) + "\t" + 
					twoDigitWholeNumberFormatter.format(target.getDecDeg()) + "\t" + 
					twoDigitWholeNumberFormatter.format(target.getDecMin()) + "\t" + 
					degreeSecondFormatter.format(target.getDecSec()));
		}
		p.close();
	}

	/**
	 * Write DS9 regions file for current configuration to file specified in <code>MascgenArguments</code>.
	 * 
 	 * @throws FileNotFoundException if File cannot be created
	 */
	public void writeDS9Regions() throws FileNotFoundException {
		writeDS9Regions(mascgenArgs.getFullPathOutputDS9Regions());
	}

	/**
	 * Write DS9 regions file for current configuration to specified file.
	 * 
	 * @param  outputFile            String path to file to write to
 	 * @throws FileNotFoundException if File cannot be created from <code>outputFile</code>
	 */
	public void writeDS9Regions(String outputFile) throws FileNotFoundException {
		FileOutputStream out = new FileOutputStream(outputFile);
		PrintStream p = new PrintStream( out );

		RaDec cp = mascgenResult.getCenter(); 
		double positionAngle = mascgenResult.getPositionAngle();
		double positionAngleInRadians = Math.toRadians(positionAngle);

		//. xCenter below maybe should be negative
		RaDec oldRedBoxCoord = 	new RaDec((cp.getXCoordinate() + mascgenArgs.getxCenter() * 60), (cp.getYCoordinate()));
		double redBoxCSUx = oldRedBoxCoord.getXCoordinate() - cp.getXCoordinate();
		double redBoxCSUy = oldRedBoxCoord.getYCoordinate() - cp.getYCoordinate();
		double theta = -positionAngleInRadians;
		double redBoxCSUxRotated = redBoxCSUx * Math.cos(theta) - redBoxCSUy * Math.sin(theta);
		double redBoxCSUyRotated = redBoxCSUx * Math.sin(theta) + redBoxCSUy * Math.cos(theta);
		double redBoxWCSxRotated = redBoxCSUxRotated + cp.getXCoordinate();
		double redBoxWCSyRotated = redBoxCSUyRotated + cp.getYCoordinate();

		/*	int roundPa = (int) Math.round(positionAngle/90.0);   	
  	double usePa = Math.abs(positionAngle - 90*roundPa);

  	double deltaX = 5*Math.sin(usePa);
  	double deltaY = 5*Math.cos(usePa); */

		double deltaX = 5*Math.cos(positionAngleInRadians);
		double deltaY = 5*Math.sin(positionAngleInRadians);

		p.println("global color=cyan font=\"helvetica 10 bold\" " +
			"width=2 select=1 highlite=0 edit=0 move=0 delete=1 " +
			"include=1 fixed=0 source \nfk5");

		double xScaleFactor = Math.cos(Math.toRadians(cp.getYCoordinate()) / 3600);
		
		//make the corners of the baffle circle

		//panda x y startangle stopangle nangle inner outer nradius

		double startAngle = Math.acos(CSU_WIDTH/2 / CSU_FP_RADIUS)*180/Math.PI ;
		double stopAngle = (90 - Math.acos(CSU_WIDTH/2 / CSU_FP_RADIUS)*180/Math.PI);

		
		
		for (int ij=0; ij<4 ; ij++){
			p.println("panda(" + 
				fiveDigitFormatter.format((cp.getXCoordinate()/ xScaleFactor / 3600))+ "," +
				fiveDigitFormatter.format(cp.getYCoordinate() / 3600)+ "," + 
				twoDigitFormatter.format(startAngle + positionAngle + 90 * ij) + "," + 
				twoDigitFormatter.format(stopAngle + positionAngle+ 90 * ij) + ",1,"+	CSU_FP_RADIUS+"\"," + (CSU_FP_RADIUS)+"\",1) #color=magenta ");
			
			p.println("line(" +
					fiveDigitFormatter.format(((cp.getXCoordinate() + CSU_FP_RADIUS * Math.sin(Math.toRadians(positionAngle - startAngle + 90 * ij)))/ xScaleFactor / 3600))+ "," +
					fiveDigitFormatter.format((cp.getYCoordinate() + CSU_FP_RADIUS * Math.cos(Math.toRadians(positionAngle - startAngle + 90 * ij))) / 3600)+ "," + 
					fiveDigitFormatter.format(((cp.getXCoordinate() + CSU_FP_RADIUS * Math.sin(Math.toRadians(positionAngle + startAngle + 90 * ij)))/ 	xScaleFactor / 3600))+ "," +
					fiveDigitFormatter.format((cp.getYCoordinate() + CSU_FP_RADIUS * Math.cos(Math.toRadians(positionAngle + startAngle + 90 * ij))) / 3600)+ ") #color=magenta");
		}

		p.println("box(" + 
				fiveDigitFormatter.format(redBoxWCSxRotated / xScaleFactor / 3600) + "," + 
				fiveDigitFormatter.format(redBoxWCSyRotated / 3600) + "," + 
				fiveDigitFormatter.format((mascgenArgs.getxRange() * 60)) + "\"" + "," + 
				fiveDigitFormatter.format(CSU_HEIGHT) + "\"" + "," + 
				threeDigitFormatter.format(positionAngle) + 
				")\t# color=red font=\"helvetica 15 normal\" text={}");
		p.println("# text(" + 
				fiveDigitFormatter.format(((cp.getXCoordinate() + CSU_HEIGHT/20*11 * Math.sin(positionAngleInRadians)) / xScaleFactor / 3600)) + "," + 
				fiveDigitFormatter.format((cp.getYCoordinate() + CSU_HEIGHT/20*11 * Math.cos(positionAngleInRadians)) / 3600)+ 
				")\t textangle=" + twoDigitFormatter.format(positionAngle) + 
				" \t text={PA=" +	twoDigitFormatter.format(positionAngle) + 
				"} color=cyan font=\"helvetica 15 bold\"");

		p.println("# text(" + 
				fiveDigitFormatter.format(((cp.getXCoordinate() + CSU_HEIGHT/20*12 * Math.sin(positionAngleInRadians)) / xScaleFactor / 3600)) + "," + 
				fiveDigitFormatter.format((cp.getYCoordinate() + CSU_HEIGHT/20*12 * Math.cos(positionAngleInRadians)) / 3600)+ 
				")\t textangle=" + twoDigitFormatter.format(positionAngle) +
				" \t text={Center="+twoDigitWholeNumberFormatter.format(cp.getRaHour()) + "h " + 
				twoDigitWholeNumberFormatter.format(cp.getRaMin()) + "m " + 
					degreeSecondFormatter.format(cp.getRaSec()) + "s  " +
					twoDigitWholeNumberFormatter.format(cp.getDecDeg()) + "deg " + 
					twoDigitWholeNumberFormatter.format(cp.getDecMin()) + "' " + 
					degreeSecondFormatter.format(cp.getDecSec())+  "\"" + 
			"} color=cyan font=\"helvetica 15 bold\"");

		p.println("# text(" + 
				fiveDigitFormatter.format(((cp.getXCoordinate() + CSU_HEIGHT/20*13 * Math.sin(positionAngleInRadians)) / xScaleFactor / 3600)) + "," + 
				fiveDigitFormatter.format((cp.getYCoordinate() + CSU_HEIGHT/20*13 * Math.cos(positionAngleInRadians)) / 3600) +
				")\t textangle=" + twoDigitFormatter.format(positionAngle) +
				" \t text={"+mascgenArgs.getMaskName()+"} color=cyan font=\"helvetica 15 bold\"");


		double textAngle = positionAngle+270+CSU_SLIT_TILT_ANGLE;
		int rounded = (int) Math.floor(textAngle/180);
		textAngle = textAngle - 180 * rounded + 180;

		while (textAngle > 90 && textAngle < 270) {
			textAngle = textAngle - 90;
		}

		for (ScienceSlit slit : scienceSlitList) {
			AstroObj target = slit.getTarget();
			p.println("circle(" + 
					fiveDigitFormatter.format((target.getWcsX() / xScaleFactor / 3600)) + "," + 
					fiveDigitFormatter.format((target.getWcsY() / 3600)) +",0.5\")\t# text={}");

			// The text labeling the object name
			p.println("# text(" + 
					fiveDigitFormatter.format(((target.getWcsX() + deltaX) / xScaleFactor / 3600)) + "," + 
					fiveDigitFormatter.format((target.getWcsY() / 3600) + deltaY / 3600.0) +
					") \t textangle=" + twoDigitFormatter.format(textAngle) +
					" \t text={" + target.getObjName() + "}");	

			//. calc wcsX
			Point2D.Double slitWcs = MascgenTransforms.getWcsFromRaDec(slit.getSlitRaDec(), cp.getYCoordinate());

			p.println("box(" + 
					fiveDigitFormatter.format((slitWcs.x / xScaleFactor / 3600)) + "," + 
					fiveDigitFormatter.format((slitWcs.y / 3600)) + "," + 
					fiveDigitFormatter.format(slit.getSlitWidth()) + "\"" + "," + 
					fiveDigitFormatter.format(slit.getSlitLength())	+ "\"," + 
					threeDigitFormatter.format((CSU_SLIT_TILT_ANGLE + positionAngle)) + ")\t #color=yellow");

			// The text labeling the slit Number
			p.println("# text(" + 
					fiveDigitFormatter.format(((slitWcs.x - deltaX) / xScaleFactor / 3600)) +	"," + 
					fiveDigitFormatter.format((slitWcs.y / 3600) - deltaY / 3600.0) +
					") \t textangle=" + twoDigitFormatter.format(textAngle) +	
					" \t text={"+slit.getSlitNumber() + "} color=yellow font=\"helvetica 10 normal\"");	
		}
		
		ArrayList<MechanicalSlit> tempList = new ArrayList<MechanicalSlit>(alignSlitList);
		
		Collections.sort(tempList, Collections.reverseOrder(slitPositionSorter));
			
		// now print the regions for the alignment slits
		for (MechanicalSlit alignSlit : tempList) {
			AstroObj target = alignSlit.getTarget();
			p.println("circle(" + 
					fiveDigitFormatter.format((target.getWcsX() / xScaleFactor / 3600)) + "," + 
					fiveDigitFormatter.format((target.getWcsY() / 3600)) + ",0.5\")\t# text={}");

			// The text labeling the object name
			p.println("# text(" + 
					fiveDigitFormatter.format((target.getWcsX() / xScaleFactor / 3600) + deltaX / 3600.0) + "," + 
					fiveDigitFormatter.format((target.getWcsY() / 3600) + deltaY / 3600.0) +
					") \t textangle=" + twoDigitFormatter.format(textAngle) +
					" \t text={" + target.getObjName() + "} color=red");	

			double deadSpace = mascgenArgs.getDitherSpace() + OVERLAP / 2;
			double rowRegionHeight = SINGLE_SLIT_HEIGHT - 2 * mascgenArgs.getDitherSpace();

			//. align slits WCS
			double slitY = ((deadSpace + rowRegionHeight / 2) + (MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS - alignSlit.getSlitNumber()) * (rowRegionHeight + 2 * deadSpace) - CSU_HEIGHT / 2);
		
			double slitX = target.getObjX() - (target.getObjY()- slitY)*Math.tan(MosfireParameters.CSU_SLIT_TILT_ANGLE_RADIANS);
			
			Point2D.Double alignSlitWcs = MascgenTransforms.getWcsFromCSUCoords(new Point2D.Double(slitX, slitY), mascgenResult.getCenter(), mascgenResult.getPositionAngle());

			p.println("box(" + 
					fiveDigitFormatter.format((alignSlitWcs.x / xScaleFactor / 3600)) + "," + 
					fiveDigitFormatter.format((alignSlitWcs.y / 3600)) + "," + 
					fiveDigitFormatter.format(alignSlit.getSlitWidth()) + "\"" + "," + 
					fiveDigitFormatter.format(MosfireParameters.SINGLE_SLIT_HEIGHT) + "\"," + 
					threeDigitFormatter.format((CSU_SLIT_TILT_ANGLE + positionAngle)) + ")\t #color=red");
		}
		p.close();
	}

	/**
	 * Write script for executing science mask to disk to file specified in <code>MascgenArguments</code>.
	 * 
	 * @param  setTargetsOnly        Flag for whether to send targets down to CSU controller
 	 * @throws FileNotFoundException if File cannot be created
	 */
	public void writeScienceCSUScript(boolean setTargetsOnly) throws FileNotFoundException {
		writeScienceCSUScript(mascgenArgs.getFullPathOutputMaskScript(), setTargetsOnly);
	}

	/**
	 * Write script for executing science mask to disk to specified file.
	 * 
	 * @param  outputFile            String path to file to write to
	 * @param  setTargetsOnly        Flag for whether to send targets down to CSU controller
 	 * @throws FileNotFoundException if File cannot be created from <code>outputFile</code>
	 */
	public void writeScienceCSUScript(String outputFile, boolean setTargetsOnly) throws FileNotFoundException {
		// Make the bar list with bar x-positions for each row. There is a total of
		// 92 bars in the MOSFIRE CSU, odd-numbered bars extend from the left and 
		// even-numbered bars extend from the right.

		FileOutputStream out = new FileOutputStream(outputFile);
		PrintStream p = new PrintStream(out);

		ArrayList<MechanicalSlit> tempList = new ArrayList<MechanicalSlit>(mechanicalSlitList);
		
		Collections.sort(tempList, slitPositionSorter);
		
		p.printf("modify -s mcsus setupname=\"%s\"\n", maskName);
		for (MechanicalSlit slit : tempList) {
			p.printf("modify -s mcsus b%02dtarg= %7.3f\n",slit.getRightBarNumber(),slit.getRightBarPositionInMM());
			p.printf("modify -s mcsus b%02dtarg= %7.3f\n",slit.getLeftBarNumber(),slit.getLeftBarPositionInMM());
		}
		if (!setTargetsOnly) {
			p.printf("modify -s mcsus setupinit=1\n");
		}
		p.close();

	}

	/**
	 * Write script for executing alignment mask to disk to file specified in <code>MascgenArguments</code>.
	 * 
	 * @param  setTargetsOnly        Flag for whether to send targets down to CSU controller
 	 * @throws FileNotFoundException if File cannot be created
	 */
	public void writeAlignmentCSUScript(boolean setTargetsOnly) throws FileNotFoundException {
		writeAlignmentCSUScript(mascgenArgs.getFullPathOutputAlignMaskScript(), setTargetsOnly);
	}

	/**
	 * Write script for executing alignment mask to disk to specified file.
	 * 
	 * @param  outputFile            String path to file to write to
	 * @param  setTargetsOnly        Flag for whether to send targets down to CSU controller
 	 * @throws FileNotFoundException if File cannot be created from <code>outputFile</code>
	 */
	public void writeAlignmentCSUScript(String outputFile, boolean setTargetsOnly) throws FileNotFoundException {
		// Make the bar list with bar x-positions for each row. There is a total of
		// 92 bars in the MOSFIRE CSU, odd-numbered bars extend from the left and 
		// even-numbered bars extend from the right.

		FileOutputStream out = new FileOutputStream(outputFile);
		PrintStream p = new PrintStream(out);

		@SuppressWarnings("unchecked")
		//. mechanicalSlitList if of type ArrayList<MechanicalSlit> so therefore its clone is too
		ArrayList<MechanicalSlit> tempList = (ArrayList<MechanicalSlit>)mechanicalSlitList.clone();
		
		Collections.sort(tempList, slitPositionSorter);
		
		if (tempList.size() == MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS) {
			for (MechanicalSlit alignSlit : alignSlitList) {
				//. assumes mechSlitList is sorted.
				tempList.set(alignSlit.getSlitNumber()-1, alignSlit);
			}
		} else {
			for (MechanicalSlit alignSlit : alignSlitList) {
				tempList.set(getMechSlitIndex(alignSlit.getSlitNumber()), alignSlit);
			}
		}
		
		p.printf("modify -s mcsus setupname=\"%s (align)\"\n", maskName);
		for (MechanicalSlit slit : tempList) {
			p.printf("modify -s mcsus b%02dtarg= %7.3f\n",slit.getRightBarNumber(),slit.getRightBarPositionInMM());
			p.printf("modify -s mcsus b%02dtarg= %7.3f\n",slit.getLeftBarNumber(),slit.getLeftBarPositionInMM());
		}
		if (!setTargetsOnly) {
			p.printf("modify -s mcsus setupinit=1\n");
		}
		p.close();

	}

	/**
	 * Gets the index of mechanical slit in mechanical slit list for specified slit number
	 *
	 * @param  slitNumber Slit (row) number for slit
	 * @return            Index for slit in slit list
	 */
	public int getMechSlitIndex(int slitNumber) {
		int ii=0;
		for (MechanicalSlit slit : mechanicalSlitList) {
			if (slit.getSlitNumber() == slitNumber) {
				return ii;
			}
			ii++;
		}
		return -1;
	}

	/**
	 * Write Keck-style star list for configuration to disk to specified file.
	 * 
 	 * @throws FileNotFoundException if File cannot be created from <code>file</code>
	 */
	public void writeOutStarList() throws FileNotFoundException {
		writeOutStarList(mascgenArgs.getFullPathOutputStarList());
	}

	/**
	 * Write Keck-style star list for configuration to disk to specified file.
	 * 
	 * @param  file                  String path to file to write to
 	 * @throws FileNotFoundException if File cannot be created from <code>file</code>
	 */
	public void writeOutStarList(String file) throws FileNotFoundException {
		// Write the outputStarList with the center of the field and the PA
		FileOutputStream out = new FileOutputStream(file);
		PrintStream p = new PrintStream(out);

		RaDec cp = mascgenResult.getCenter();

		//. coordinates must start in column 17
		//.                                            1234567890123456
		StringBuffer name = new StringBuffer(maskName+"                ");
		
		p.println(name.substring(0,15)+" "+twoDigitWholeNumberFormatter.format(cp.getRaHour()) + " " + 
				twoDigitWholeNumberFormatter.format(cp.getRaMin()) + " " + 
				degreeSecondFormatter.format(cp.getRaSec()) + "  " +
				twoDigitWholeNumberFormatter.format(cp.getDecDeg()) + " " + 
				twoDigitWholeNumberFormatter.format(cp.getDecMin()) + " " + 
				degreeSecondFormatter.format(cp.getDecSec())+  
				" 2000.00 rotdest="+
				twoDigitFormatter.format(mascgenResult.getPositionAngle()) +" rotmode=PA");
		p.close();
	}
	
	/**
	 * Write FITS file with FITS table extensions describing CSU mask to specified file.
	 * 
	 * @param  file          String path to file to write extensions to
	 * @param  doAlign       Flag for whether to write alignment table extension
	 * @throws IOException   on error writing to file
	 * @throws FitsException on error constructing extensions
	 */
	public void writeFITSExtension(String file, boolean doAlign) throws IOException, FitsException {
		Fits f = new Fits();
		f.addHDU(constructTargetListFITSExtension());
		f.addHDU(constructScienceSlitListFITSExtension());
		f.addHDU(constructMechanicalSlitListFITSExtension(doAlign));
		f.addHDU(constructAlignmentSlitListFITSExtension());
		BufferedFile bf = new BufferedFile(file, "rw");
		f.write(bf);
		bf.flush();
		bf.close();
	}

	/**
	 * Construct FITS table for target list.
	 * 
	 * @return               AsciiTableHDU containing target list table
	 * @throws FitsException on error constructing table
	 */
	private AsciiTableHDU constructTargetListFITSExtension() throws FitsException {
		AsciiTable table = new AsciiTable();
		
		@SuppressWarnings("unchecked")
		//. mechanicalSlitList if of type ArrayList<MechanicalSlit> so therefore its clone is too
		ArrayList<ScienceSlit> tempList = (ArrayList<ScienceSlit>)scienceSlitList.clone();
		
		Collections.sort(tempList, slitPositionSorter);
		
		int tempListSize = tempList.size() + alignSlitList.size();
		String[] targetNameColumn = new String[tempListSize];
		String[] priorityColumn = new String[tempListSize];
		String[] magnitudeColumn = new String[tempListSize];
		String[] raHColumn = new String[tempListSize];
		String[] raMColumn = new String[tempListSize];
		String[] raSColumn = new String[tempListSize];
		String[] decDColumn = new String[tempListSize];
		String[] decMColumn = new String[tempListSize];
		String[] decSColumn = new String[tempListSize];
		String[] epochColumn = new String[tempListSize];
		String[] equinoxColumn = new String[tempListSize];
		int ii=0;
		for (ScienceSlit slit : tempList) {
			AstroObj target = slit.getTarget();
			targetNameColumn[ii] = target.getObjName();
			priorityColumn[ii] = twoDigitFormatter.format(target.getObjPriority());
			magnitudeColumn[ii] = twoDigitFormatter.format(target.getObjMag());
			raHColumn[ii] = twoDigitWholeNumberFormatter.format(target.getRaHour());
			raMColumn[ii] = twoDigitWholeNumberFormatter.format(target.getRaMin());
			raSColumn[ii] = degreeSecondFormatter.format(target.getRaSec());
			decDColumn[ii] = twoDigitWholeNumberFormatter.format(target.getDecDeg());
			decMColumn[ii] = twoDigitWholeNumberFormatter.format(target.getDecMin());
			decSColumn[ii] = degreeSecondFormatter.format(target.getDecSec());
			epochColumn[ii] = oneDigitFormatter.format(target.getEpoch());
			equinoxColumn[ii] = oneDigitFormatter.format(target.getEquinox());
			ii++;
		}

		for (MechanicalSlit alignSlit : alignSlitList) {
			AstroObj target = alignSlit.getTarget();
			targetNameColumn[ii] = target.getObjName();
			priorityColumn[ii] = twoDigitFormatter.format(target.getObjPriority());
			magnitudeColumn[ii] = twoDigitFormatter.format(target.getObjMag());
			raHColumn[ii] = twoDigitWholeNumberFormatter.format(target.getRaHour());
			raMColumn[ii] = twoDigitWholeNumberFormatter.format(target.getRaMin());
			raSColumn[ii] = degreeSecondFormatter.format(target.getRaSec());
			decDColumn[ii] = twoDigitWholeNumberFormatter.format(target.getDecDeg());
			decMColumn[ii] = twoDigitWholeNumberFormatter.format(target.getDecMin());
			decSColumn[ii] = degreeSecondFormatter.format(target.getDecSec());
			epochColumn[ii] = oneDigitFormatter.format(target.getEpoch());
			equinoxColumn[ii] = oneDigitFormatter.format(target.getEquinox());			
			ii++;
		}

		table.addColumn(targetNameColumn);
		table.addColumn(priorityColumn);
		table.addColumn(magnitudeColumn);
		table.addColumn(raHColumn);
		table.addColumn(raMColumn);
		table.addColumn(raSColumn);
		table.addColumn(decDColumn);
		table.addColumn(decMColumn);
		table.addColumn(decSColumn);
		table.addColumn(epochColumn);
		table.addColumn(equinoxColumn);

		Header h = AsciiTableHDU.manufactureHeader(table);
		
		h.addValue("EXTNAME", "Target_List", "Table name");
		h.addValue("TTYPE1", "Target_Name", "Label for field");
		h.addValue("TTYPE2", "Priority", "Label for field");
		h.addValue("TTYPE3", "Magnitude", "Label for field");
		h.addValue("TTYPE4", "RA_Hours", "Label for field");
		h.addValue("TTYPE5", "RA_Minutes", "Label for field");
		h.addValue("TTYPE6", "RA_Seconds", "Label for field");
		h.addValue("TTYPE7", "Dec_Degrees", "Label for field");
		h.addValue("TTYPE8", "Dec_Minutes", "Label for field");
		h.addValue("TTYPE9", "Dec_Seconds", "Label for field");
		h.addValue("TTYPE10", "Epoch", "Label for field");
		h.addValue("TTYPE11", "Equinox", "Label for field");
		
		
		return new AsciiTableHDU(h, table);
	}

	/**
	 * Construct FITS table for science slit list.
	 * 
	 * @return               AsciiTableHDU containing science slit list table
	 * @throws FitsException on error constructing table
	 */
	private AsciiTableHDU constructScienceSlitListFITSExtension() throws FitsException {
		AsciiTable table = new AsciiTable();
		@SuppressWarnings("unchecked")
		//. mechanicalSlitList if of type ArrayList<MechanicalSlit> so therefore its clone is too
		ArrayList<ScienceSlit> tempList = (ArrayList<ScienceSlit>)scienceSlitList.clone();
		
		Collections.sort(tempList, slitPositionSorter);
		
		int tempListSize = tempList.size() + alignSlitList.size();
		String[] slitNumberColumn = new String[tempListSize];
		String[] raHColumn = new String[tempListSize];
		String[] raMColumn = new String[tempListSize];
		String[] raSColumn = new String[tempListSize];
		String[] decDColumn = new String[tempListSize];
		String[] decMColumn = new String[tempListSize];
		String[] decSColumn = new String[tempListSize];
		String[] slitWidthColumn = new String[tempListSize];
		String[] slitLengthColumn = new String[tempListSize];
		String[] centerDistanceColumn = new String[tempListSize];
		String[] targetNameColumn = new String[tempListSize];
		String[] priorityColumn = new String[tempListSize];
		int ii=0;
		for (ScienceSlit slit : tempList) {
			slitNumberColumn[ii] = Integer.toString(slit.getSlitNumber());
			RaDec slitRaDec = slit.getSlitRaDec();
			raHColumn[ii] = twoDigitWholeNumberFormatter.format(slitRaDec.getRaHour());
			raMColumn[ii] = twoDigitWholeNumberFormatter.format(slitRaDec.getRaMin());
			raSColumn[ii] = degreeSecondFormatter.format(slitRaDec.getRaSec());
			decDColumn[ii] = twoDigitWholeNumberFormatter.format(slitRaDec.getDecDeg());
			decMColumn[ii] = twoDigitWholeNumberFormatter.format(slitRaDec.getDecMin());
			decSColumn[ii] = degreeSecondFormatter.format(slitRaDec.getDecSec());
			slitWidthColumn[ii] = threeDigitFormatter.format(slit.getSlitWidth());
			slitLengthColumn[ii] = threeDigitFormatter.format(slit.getSlitLength());
			centerDistanceColumn[ii] = threeDigitFormatter.format(slit.getCenterDistance());
			AstroObj target = slit.getTarget();
			targetNameColumn[ii] = target.getObjName();
			priorityColumn[ii] = twoDigitFormatter.format(target.getObjPriority());
			ii++;
		}


		
		table.addColumn(slitNumberColumn);
		table.addColumn(raHColumn);
		table.addColumn(raMColumn);
		table.addColumn(raSColumn);
		table.addColumn(decDColumn);
		table.addColumn(decMColumn);
		table.addColumn(decSColumn);
		table.addColumn(slitWidthColumn);
		table.addColumn(slitLengthColumn);
		table.addColumn(centerDistanceColumn);
		table.addColumn(targetNameColumn);
		table.addColumn(priorityColumn);

		Header h = AsciiTableHDU.manufactureHeader(table);
		
		h.addValue("EXTNAME", "Science_Slit_List", "Table name");
		h.addValue("TTYPE1", "Slit_Number", "Label for field");
		h.addValue("TTYPE2", "Slit_RA_Hours", "Label for field");
		h.addValue("TTYPE3", "Slit_RA_Minutes", "Label for field");
		h.addValue("TTYPE4", "Slit_RA_Seconds", "Label for field");
		h.addValue("TTYPE5", "Slit_Dec_Degrees", "Label for field");
		h.addValue("TTYPE6", "Slit_Dec_Minutes", "Label for field");
		h.addValue("TTYPE7", "Slit_Dec_Seconds", "Label for field");
		h.addValue("TTYPE8", "Slit_width", "Label for field");
		h.addValue("TUNIT8", "arcsec", "Physical units for field");
		h.addValue("TTYPE9", "Slit_length", "Label for field");
		h.addValue("TUNIT9", "arcsec", "Physical units for field");
		h.addValue("TTYPE10", "Target_to_center_of_slit_distance", "Label for field");
		h.addValue("TUNIT10", "arcsec", "Physical units for field");
		h.addValue("TTYPE11", "Target_Name", "Label for field");
		h.addValue("TTYPE12", "Target_Priority", "Label for field");
		
		return new AsciiTableHDU(h, table);
		
	}
	
	/**
	 * Construct FITS table for mechanical slit list.
	 * 
	 * @return               AsciiTableHDU containing mechanical slit list table
	 * @throws FitsException on error constructing table
	 */
	private AsciiTableHDU constructMechanicalSlitListFITSExtension(boolean doAlign) throws FitsException {
		AsciiTable table = new AsciiTable();
		@SuppressWarnings("unchecked")
		//. mechanicalSlitList if of type ArrayList<MechanicalSlit> so therefore its clone is too
		ArrayList<MechanicalSlit> tempList = (ArrayList<MechanicalSlit>)mechanicalSlitList.clone();
		
		Collections.sort(tempList, slitPositionSorter);
		
		if (doAlign) {
			if (tempList.size() == MosfireParameters.CSU_NUMBER_OF_BAR_PAIRS) {
				for (MechanicalSlit alignSlit : alignSlitList) {
					//. assumes mechSlitList is sorted.
					tempList.set(alignSlit.getSlitNumber()-1, alignSlit);
				}
			} else {
				for (MechanicalSlit alignSlit : alignSlitList) {
					tempList.set(getMechSlitIndex(alignSlit.getSlitNumber()), alignSlit);
				}
			}			
		}
		int tempListSize = tempList.size();
		String[] slitNumberColumn = new String[tempListSize];
		String[] targetNameColumn = new String[tempListSize];
		String[] priorityColumn = new String[tempListSize];
		String[] centerPositionColumn = new String[tempListSize];
		String[] slitWidthColumn = new String[tempListSize];
		String[] centerDistanceColumn = new String[tempListSize];
		int ii=0;
		for (MechanicalSlit slit : tempList) {
			slitNumberColumn[ii] = Integer.toString(slit.getSlitNumber());
			targetNameColumn[ii] = slit.getTargetName();
			priorityColumn[ii] = twoDigitFormatter.format(slit.getTarget().getObjPriority());
			centerPositionColumn[ii] = threeDigitFormatter.format(slit.getCenterPosition());
			slitWidthColumn[ii] = threeDigitFormatter.format(slit.getSlitWidth());
			centerDistanceColumn[ii] = threeDigitFormatter.format(slit.getCenterDistance());
			ii++;
		}
		
		table.addColumn(slitNumberColumn);
		table.addColumn(targetNameColumn);
		table.addColumn(priorityColumn);
		table.addColumn(centerPositionColumn);
		table.addColumn(slitWidthColumn);
		table.addColumn(centerDistanceColumn);
		
		Header h = AsciiTableHDU.manufactureHeader(table);
		
		h.addValue("EXTNAME", "Mechanical_Slit_List", "Table name");
		h.addValue("TTYPE1", "Slit_Number", "Label for field");
		h.addValue("TTYPE2", "Target_in_Slit", "Label for field");
		h.addValue("TTYPE3", "Target_Priority", "Label for field");
		h.addValue("TTYPE4", "Position_of_Slit", "Label for field");
		h.addValue("TUNIT4", "arcsec", "Physical units for field");
		h.addValue("TTYPE5", "Slit_width", "Label for field");
		h.addValue("TUNIT5", "arcsec", "Physical units for field");
		h.addValue("TTYPE6", "Target_to_center_of_slit_distance", "Label for field");
		h.addValue("TUNIT6", "arcsec", "Physical units for field");
		
		
		return new AsciiTableHDU(h, table);
		
	}

	/**
	 * Construct FITS table for alignment slit list.
	 * 
	 * @return               AsciiTableHDU containing alignment slit list table
	 * @throws FitsException on error constructing table
	 */
	private AsciiTableHDU constructAlignmentSlitListFITSExtension() throws FitsException {
		AsciiTable table = new AsciiTable();
		@SuppressWarnings("unchecked")
		//. mechanicalSlitList if of type ArrayList<MechanicalSlit> so therefore its clone is too
		ArrayList<MechanicalSlit> tempList = (ArrayList<MechanicalSlit>)alignSlitList.clone();
		
		Collections.sort(tempList, slitPositionSorter);

		int tempListSize = tempList.size();
		String[] slitNumberColumn = new String[tempListSize];
		String[] centerPositionColumn = new String[tempListSize];
		String[] slitWidthColumn = new String[tempListSize];
		String[] centerDistanceColumn = new String[tempListSize];
		String[] targetNameColumn = new String[tempListSize];
		String[] priorityColumn = new String[tempListSize];
		String[] magnitudeColumn = new String[tempListSize];
		String[] raHColumn = new String[tempListSize];
		String[] raMColumn = new String[tempListSize];
		String[] raSColumn = new String[tempListSize];
		String[] decDColumn = new String[tempListSize];
		String[] decMColumn = new String[tempListSize];
		String[] decSColumn = new String[tempListSize];
		String[] epochColumn = new String[tempListSize];
		String[] equinoxColumn = new String[tempListSize];
		
		int ii=0;
		for (MechanicalSlit slit : tempList) {
			slitNumberColumn[ii] = Integer.toString(slit.getSlitNumber());
			centerPositionColumn[ii] = threeDigitFormatter.format(slit.getCenterPosition());
			slitWidthColumn[ii] = threeDigitFormatter.format(slit.getSlitWidth());
			centerDistanceColumn[ii] = threeDigitFormatter.format(slit.getCenterDistance());
			targetNameColumn[ii] = slit.getTargetName();
			AstroObj target = slit.getTarget();
			priorityColumn[ii] = twoDigitFormatter.format(target.getObjPriority());
			magnitudeColumn[ii] = twoDigitFormatter.format(target.getObjMag());
			raHColumn[ii] = twoDigitWholeNumberFormatter.format(target.getRaHour());
			raMColumn[ii] = twoDigitWholeNumberFormatter.format(target.getRaMin());
			raSColumn[ii] = degreeSecondFormatter.format(target.getRaSec());
			decDColumn[ii] = twoDigitWholeNumberFormatter.format(target.getDecDeg());
			decMColumn[ii] = twoDigitWholeNumberFormatter.format(target.getDecMin());
			decSColumn[ii] = degreeSecondFormatter.format(target.getDecSec());
			epochColumn[ii] = oneDigitFormatter.format(target.getEpoch());
			equinoxColumn[ii] = oneDigitFormatter.format(target.getEquinox());
			ii++;
		}
		
		table.addColumn(slitNumberColumn);
		table.addColumn(centerPositionColumn);
		table.addColumn(slitWidthColumn);
		table.addColumn(centerDistanceColumn);
		table.addColumn(targetNameColumn);
		table.addColumn(priorityColumn);
		table.addColumn(magnitudeColumn);
		table.addColumn(raHColumn);
		table.addColumn(raMColumn);
		table.addColumn(raSColumn);
		table.addColumn(decDColumn);
		table.addColumn(decMColumn);
		table.addColumn(decSColumn);
		table.addColumn(epochColumn);
		table.addColumn(equinoxColumn);

		
		Header h = AsciiTableHDU.manufactureHeader(table);
		
		h.addValue("EXTNAME", "Alignment_Slit_List", "Table name");
		h.addValue("TTYPE1", "Slit_Number", "Label for field");
		h.addValue("TTYPE2", "Position_of_Slit", "Label for field");
		h.addValue("TUNIT2", "arcsec", "Physical units for field");
		h.addValue("TTYPE3", "Slit_width", "Label for field");
		h.addValue("TUNIT3", "arcsec", "Physical units for field");
		h.addValue("TTYPE4", "Target_to_center_of_slit_distance", "Label for field");
		h.addValue("TUNIT4", "arcsec", "Physical units for field");
		h.addValue("TTYPE5", "Target_in_Slit", "Label for field");
		h.addValue("TTYPE6", "Target_Priority", "Label for field");
		h.addValue("TTYPE7", "Target_Magnitude", "Label for field");
		h.addValue("TTYPE8", "Target_RA_Hours", "Label for field");
		h.addValue("TTYPE9", "Target_RA_Minutes", "Label for field");
		h.addValue("TTYPE10", "Target_RA_Seconds", "Label for field");
		h.addValue("TTYPE11", "Target_Dec_Degrees", "Label for field");
		h.addValue("TTYPE12", "Target_Dec_Minutes", "Label for field");
		h.addValue("TTYPE13", "Target_Dec_Seconds", "Label for field");
		h.addValue("TTYPE14", "Target_Epoch", "Label for field");
		h.addValue("TTYPE15", "Target_Equinox", "Label for field");

		
		return new AsciiTableHDU(h, table);
		
	}

	/**
	 * Gets all targets in configuration including alignment stars.
	 *
	 * @return AstroObj ArrayList containing all targets
	 */
	public ArrayList<AstroObj> getAllTargets() {
		ArrayList<AstroObj> list = new ArrayList<AstroObj>();
		for (ScienceSlit slit : scienceSlitList) {
			list.add(slit.getTarget());
		}
		for (MechanicalSlit alignSlit: alignSlitList) {
			list.add(alignSlit.getTarget());
		}
		return list;
	}

	public ArrayList<MechanicalSlit> getMechanicalSlitList() {
		return mechanicalSlitList;
	}

	public void setMechanicalSlitList(ArrayList<MechanicalSlit> slitList) {
		this.mechanicalSlitList = slitList;
	}

	public ArrayList<ScienceSlit> getScienceSlitList() {
		return scienceSlitList;
	}

	public void setScienceSlitList(ArrayList<ScienceSlit> scienceSlitList) {
		this.scienceSlitList = scienceSlitList;
	}

	public ArrayList<MechanicalSlit> getAlignSlitList() {
		return alignSlitList;
	}

	public void setAlignSlitList(ArrayList<MechanicalSlit> alignSlitList) {
		this.alignSlitList = alignSlitList;
	}

	public MascgenArguments getMascgenArgs() {
		return mascgenArgs;
	}

	public void setMascgenArgs(MascgenArguments mascgenArgs) {
		this.mascgenArgs = mascgenArgs;
	}

	public MascgenResult getMascgenResult() {
		return mascgenResult;
	}

	public void setMascgenResult(MascgenResult mascgenResult) {
		this.mascgenResult = mascgenResult;
	}

	public void setOriginalTargetSet(List<AstroObj> originalTargetList) {
		this.originalTargetList = originalTargetList;
	}
	public List<AstroObj> getOriginalTargetList() {
		return originalTargetList;
	}

	// Part of MAGMA UPGRADE M4 by Ji Man Sohn, UCLA 2016-2017
	public void setExcessTargetSet(List<AstroObj> originalTargetList) {
		this.excessTargetList = originalTargetList;
	}
  	// Part of MAGMA UPGRADE M4 by Ji Man Sohn, UCLA 2016-2017
	public List<AstroObj> getExcessTargetList() {
		return excessTargetList;
	}
	public String getMaskName() {
		return maskName;
	}

	public void setMaskName(String maskName) {
		this.maskName = maskName;
		mascgenArgs.setMaskName(maskName);
		status = STATUS_MODIFIED;
	}
	public int getAlignmentStarCount() {
		return alignSlitList.size();
	}
	public String getVersion() {
		return mscVersion;
	}
	
	private void setVersion(String version) {
		this.mscVersion = version;
	}

	public String getStatus() {
		return status;
	}
	
	private void setStatus(String status) {
		this.status = status;
	}
	private void setOriginalFilename(String name) {
		this.originalFilename = name;
	}
	public String getOriginalFilename() {
		return originalFilename;
	}
  	// Part of MAGMA UPGRADE M4 by Ji Man Sohn, UCLA 2016-2017
	public void writeExcessCoordsFile() throws FileNotFoundException {
		writeExcessCoordsFile(mascgenArgs.getFullPathOutputExcessTargets());
	}
  	// Part of MAGMA UPGRADE M4 by Ji Man Sohn, UCLA 2016-2017
	public void writeExcessCoordsFile(String fileName) throws FileNotFoundException {
		FileOutputStream out = new FileOutputStream(fileName);
		PrintStream p = new PrintStream(out);
		
		List<AstroObj> unused = new ArrayList<AstroObj>();
		List<AstroObj> used = getAllTargets();
		
		for(AstroObj target : originalTargetList) {
			if(!isPresent(target, used)) {
				unused.add(target);
				p.printf("%s  %7.2f  %5.2f  %02.0f  %02.0f  %06.3f  % 03.0f  %02.0f  %05.2f  %6.1f  %6.1f  %3.1f  %3.1f\n", 
						target.getObjName(), target.getObjPriority(), 
						target.getObjMag(), target.getRaHour(), target.getRaMin(), 
						target.getRaSec(), target.getDecDeg(), target.getDecMin(), 
						target.getDecSec(), target.getEpoch(), target.getEquinox(), 0.0, 0.0);			
			}
		}
		setExcessTargetSet(unused);
		p.close();
	}
	// Part of MAGMA UPGRADE M4 by Ji Man Sohn, UCLA 2016-2017
	private boolean isPresent(Object obj, List<AstroObj> list){
		for (AstroObj a : list){
			if(a.equals(obj)){
				return true;
			}
		}
		return false;
	}
}
