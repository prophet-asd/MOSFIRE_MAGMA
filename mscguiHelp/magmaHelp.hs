<?xml version='1.0' encoding='ISO-8859-1' ?>
<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 2.0//EN"
         "../dtd/helpset_2_0.dtd">

<helpset version="1.0">

  <!-- title -->
  <title>MAGMA: The MOSFIRE Slit Configuration Tool - Help</title>

  <!-- maps -->
  <maps>
     <homeID>mscgui.helpintro</homeID>
     <mapref location="Map.jhm"/>
  </maps>

  <!-- views -->
  <view>
    <name>TOC</name>
    <label>Table Of Contents</label>
    <type>javax.help.TOCView</type>
    <data>mscguiHelpTOC.xml</data>
  </view>

  <view>
    <name>Index</name>
    <label>Index</label>
    <type>javax.help.IndexView</type>
    <data>mscguiHelpIndex.xml</data>
  </view>

  <view>
    <name>Search</name>
    <label>Search</label>
    <type>javax.help.SearchView</type>
    <data engine="com.sun.java.help.search.DefaultSearchEngine">
      JavaHelpSearch
    </data>
  </view>

  <view>
    <name>Favorites</name>
    <label>Favorites</label>
    <type>javax.help.FavoritesView</type>
  </view>

  <presentation default="true" displayviewimages="false">
     <name>main window</name>
     <size width="1150" height="600" />
     <location x="200" y="200" />
     <title>MAGMA - Online Help</title>
     <image>toplevelfolder</image>
     <toolbar>
	<helpaction>javax.help.BackAction</helpaction>
	<helpaction>javax.help.ForwardAction</helpaction>
	<helpaction>javax.help.SeparatorAction</helpaction>
	<helpaction>javax.help.HomeAction</helpaction>
	<helpaction>javax.help.ReloadAction</helpaction>
	<helpaction>javax.help.SeparatorAction</helpaction>
	<helpaction>javax.help.PrintAction</helpaction>
	<helpaction>javax.help.PrintSetupAction</helpaction>
     </toolbar>
  </presentation>
  <presentation displayviews=false>
     <name>external</name>
     <size width="800" height="600" />
     <location x="200" y="200" />
     <title>MAGMA - Online Help</title>
  </presentation>
</helpset>
