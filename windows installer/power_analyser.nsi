;NSIS Modern User Interface

  !define APP_NAME "Log4 Analyser"
  !define COMPANY_NAME "Tektyte"
  !define ICON_FILENAME "Tektyte.ico"
  !define JAR_FILENAME "Log4_Analyser.jar"
  !define JAVA_FOLDERNAME "jre7"
  !define BAT_SCRIPT_FILENAME "Log4_Analyser.bat"
  
  ; These three must be integers
  !define VERSIONMAJOR 0
  !define VERSIONMINOR 1
  !define VERSIONBUILD 210

  ; This is the size (in kB) of all the files copied into "Program Files"
  !define INSTALLSIZE 127000
  
  ; java installer 
  ; !define JRE_VERSION "1.6"
  ; !define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=52252"
  ; !define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=94214"
;--------------------------------
;Include Modern UI

  !include "MUI2.nsh"
  
  ; java installer
  ; !include "JREDyna_Inetc.nsh" 

;--------------------------------
;General

  ;Name and output file initialisastion
  Name "${APP_NAME}"
  OutFile "${APP_NAME}.exe"

  ;Default installation folder
  InstallDir "$PROGRAMFILES\${COMPANY_NAME}\${APP_NAME}"
  
  ;Get installation folder from registry if available
  ;InstallDirRegKey HKCU "Software\Modern UI Test" ""

  ;Request application privileges for Windows Vista
  ;RequestExecutionLevel user

;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING

;--------------------------------
;Pages
  ; Location of the license file (current directory)
  !insertmacro MUI_PAGE_LICENSE "license.txt"
  
  ; !insertmacro CUSTOM_PAGE_JREINFO
  ; used to install java if it is required
  
  ; Page custom JAVA_PAGE
  ; !insertmacro MUI_PAGE_COMPONENTS
  
  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_INSTFILES
  
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES
  
;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Installer Sections

Section "Required" InstallSection
  SetShellVarContext all
  SetOutPath "$INSTDIR"
  
  ; Files included with the installer
  
  ; java version 1.7
  File /r "${JAVA_FOLDERNAME}"
  
  ; bat script for running jar with the java version included
  File "${BAT_SCRIPT_FILENAME}"
  
  ; the runnable .jar
  File "${JAR_FILENAME}"
  
  ; icon for the link
  File "${ICON_FILENAME}"
  
  ; start menu short-cut setup
  CreateDirectory "$SMPROGRAMS\${COMPANY_NAME}"
  CreateShortCut "$SMPROGRAMS\${COMPANY_NAME}\${APP_NAME}.lnk" "$INSTDIR\${BAT_SCRIPT_FILENAME}" "" "$INSTDIR\${ICON_FILENAME}"

  ;Store installation folder
  WriteRegStr HKCU "Software\Modern UI Test" "" $INSTDIR
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  
  ; Registry information for add/remove programs
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "DisplayName" "${COMPANY_NAME} - ${APP_NAME}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "QuietUninstallString" "$\"$INSTDIR\uninstall.exe$\" /S"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "InstallLocation" "$\"$INSTDIR$\""
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "DisplayIcon" "$\"$INSTDIR\${ICON_FILENAME}$\""
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "Publisher" "$\"${COMPANY_NAME}$\""
  ;WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "HelpLink" "$\"${HELPURL}$\""
  ;WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "URLUpdateInfo" "$\"${UPDATEURL}$\""
  ;WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "URLInfoAbout" "$\"${ABOUTURL}$\""
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "DisplayVersion" "$\"${VERSIONMAJOR}.${VERSIONMINOR}.${VERSIONBUILD}$\""
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "VersionMajor" ${VERSIONMAJOR}
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "VersionMinor" ${VERSIONMINOR}
  ; There is no option for modifying or repairing the install
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "NoRepair" 1
  ; Set the INSTALLSIZE constant (!defined at the top of this script) so Add/Remove Programs can accurately report the size
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}" "EstimatedSize" ${INSTALLSIZE}

SectionEnd
;--------------------------------
;Descriptions

  ;Language strings
  LangString DESC_InstallSec ${LANG_ENGLISH} "The only section."

  ;Assign language strings to sections
  !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${InstallSection} $(DESC_InstallSec)
  !insertmacro MUI_FUNCTION_DESCRIPTION_END

;--------------------------------
;Uninstaller Section

Section "Uninstall"

  SetShellVarContext all
  ; Remove Start Menu launcher
  Delete "$SMPROGRAMS\${COMPANY_NAME}\${APP_NAME}.lnk"
  
  ; Try to remove the Start Menu folder - this will only happen if it is empty
  rmDir /r "$SMPROGRAMS\${COMPANY_NAME}"

  ; Remove files
  rmDir /r $INSTDIR\${JAVA_FOLDERNAME}
  Delete $INSTDIR\${JAR_FILENAME}
  Delete $INSTDIR\${BAT_SCRIPT_FILENAME}
  Delete $INSTDIR\${ICON_FILENAME}	

  ; remove the uninstall file
  Delete "$INSTDIR\Uninstall.exe"
	
  ; remove the install directory folder	
  RMDir "$INSTDIR"

  ; DeleteRegKey /ifempty HKCU "Software\Modern UI Test"
  ; Remove uninstaller information from the registry
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${COMPANY_NAME} ${APP_NAME}"
  
SectionEnd

; Functions
; Function JAVA_PAGE
;	Call DownloadAndInstallJREIfNecessary
; FunctionEnd