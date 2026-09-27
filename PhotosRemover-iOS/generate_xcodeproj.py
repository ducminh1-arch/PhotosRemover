import os
import sys

def generate_xcodeproj(base_dir):
    xcodeproj_dir = os.path.join(base_dir, "PhotosRemover.xcodeproj")
    xcworkspace_dir = os.path.join(xcodeproj_dir, "project.xcworkspace")
    xcschemes_dir = os.path.join(xcodeproj_dir, "xcshareddata", "xcschemes")
    
    os.makedirs(xcodeproj_dir, exist_ok=True)
    os.makedirs(xcworkspace_dir, exist_ok=True)
    os.makedirs(xcschemes_dir, exist_ok=True)
    
    # 1. contents.xcworkspacedata
    workspace_data = """<?xml version="1.0" encoding="UTF-8"?>
<Workspace
   version = "1.0">
   <FileRef
      location = "self:">
   </FileRef>
</Workspace>
"""
    with open(os.path.join(xcworkspace_dir, "contents.xcworkspacedata"), "w", encoding="utf-8") as f:
        f.write(workspace_data)
        
    # 2. PhotosRemover.xcscheme
    scheme_data = """<?xml version="1.0" encoding="UTF-8"?>
<Scheme
   LastUpgradeVersion = "1500"
   version = "1.7">
   <BuildAction
      parallelizeBuildables = "YES"
      buildImplicitDependencies = "YES"
      runPostActionsOnFailure = "NO">
      <BuildActionEntries>
         <BuildActionEntry
            buildForTesting = "YES"
            buildForRunning = "YES"
            buildForProfiling = "YES"
            buildForArchiving = "YES"
            buildForAnalyzing = "YES">
            <BuildableReference
               BuildableIdentifier = "primary"
               BlueprintIdentifier = "A10000000000000000000001"
               BuildableName = "Remo.app"
               BlueprintName = "PhotosRemover"
               ReferencedContainer = "container:PhotosRemover.xcodeproj">
            </BuildableReference>
         </BuildActionEntry>
         <BuildActionEntry
            buildForTesting = "YES"
            buildForRunning = "NO"
            buildForProfiling = "NO"
            buildForArchiving = "NO"
            buildForAnalyzing = "NO">
            <BuildableReference
               BuildableIdentifier = "primary"
               BlueprintIdentifier = "A10000000000000000000002"
               BuildableName = "PhotosRemoverTests.xctest"
               BlueprintName = "PhotosRemoverTests"
               ReferencedContainer = "container:PhotosRemover.xcodeproj">
            </BuildableReference>
         </BuildActionEntry>
      </BuildActionEntries>
   </BuildAction>
   <TestAction
      buildConfiguration = "Debug"
      selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB"
      selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB"
      shouldUseLaunchSchemeArgsEnv = "YES"
      shouldAutocreateTestPlan = "YES">
      <Testables>
         <TestableReference
            skipped = "NO">
            <BuildableReference
               BuildableIdentifier = "primary"
               BlueprintIdentifier = "A10000000000000000000002"
               BuildableName = "PhotosRemoverTests.xctest"
               BlueprintName = "PhotosRemoverTests"
               ReferencedContainer = "container:PhotosRemover.xcodeproj">
            </BuildableReference>
         </TestableReference>
      </Testables>
   </TestAction>
   <LaunchAction
      buildConfiguration = "Debug"
      selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB"
      selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB"
      launchStyle = "0"
      useCustomWorkingDirectory = "NO"
      ignoresPersistentStateOnLaunch = "NO"
      debugDocumentVersioning = "YES"
      debugServiceExtension = "internal"
      allowLocationSimulation = "YES">
      <BuildableProductRunnable
         runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "A10000000000000000000001"
            BuildableName = "Remo.app"
            BlueprintName = "PhotosRemover"
            ReferencedContainer = "container:PhotosRemover.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </LaunchAction>
   <ProfileAction
      buildConfiguration = "Release"
      shouldUseLaunchSchemeArgsEnv = "YES"
      savedToolIdentifier = ""
      useCustomWorkingDirectory = "NO"
      debugDocumentVersioning = "YES">
      <BuildableProductRunnable
         runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "A10000000000000000000001"
            BuildableName = "Remo.app"
            BlueprintName = "PhotosRemover"
            ReferencedContainer = "container:PhotosRemover.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </ProfileAction>
   <AnalyzeAction
      buildConfiguration = "Debug">
   </AnalyzeAction>
   <ArchiveAction
      buildConfiguration = "Release"
      revealArchiveInOrganizer = "YES">
   </ArchiveAction>
</Scheme>
"""
    with open(os.path.join(xcschemes_dir, "PhotosRemover.xcscheme"), "w", encoding="utf-8") as f:
        f.write(scheme_data)
        
    # 3. project.pbxproj
    # Map all files
    # ID scheme:
    # A0... Project, Groups
    # A1... Targets
    # B0... Sub-Groups
    # C0... File References
    # D0... Build Files
    # E0... Build Phases
    # F0... Configurations
    
    pbxproj_content = """// !$*UTF8*$!
{
	archiveVersion = 1;
	classes = {
	};
	objectVersion = 56;
	objects = {

/* Begin PBXBuildFile section */
		D00000000000000000000001 /* PhotosRemoverApp.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000001 /* PhotosRemoverApp.swift */; };
		D00000000000000000000002 /* DuplicateSet.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000002 /* DuplicateSet.swift */; };
		D00000000000000000000003 /* PhotoItem.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000003 /* PhotoItem.swift */; };
		D00000000000000000000004 /* ScanProgress.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000004 /* ScanProgress.swift */; };
		D00000000000000000000005 /* SimilarSensitivity.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000005 /* SimilarSensitivity.swift */; };
		D00000000000000000000006 /* StorageBreakdown.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000006 /* StorageBreakdown.swift */; };
		D00000000000000000000007 /* CleanedStatsStore.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000007 /* CleanedStatsStore.swift */; };
		D00000000000000000000008 /* DeviceStorageService.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000008 /* DeviceStorageService.swift */; };
		D00000000000000000000009 /* DuplicateEngine.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000009 /* DuplicateEngine.swift */; };
		D0000000000000000000000A /* ExclusionStore.swift in Sources */ = {isa = PBXBuildFile; fileRef = C0000000000000000000000A /* ExclusionStore.swift */; };
		D0000000000000000000000B /* PhotoLibraryService.swift in Sources */ = {isa = PBXBuildFile; fileRef = C0000000000000000000000B /* PhotoLibraryService.swift */; };
		D0000000000000000000000C /* MainViewModel.swift in Sources */ = {isa = PBXBuildFile; fileRef = C0000000000000000000000C /* MainViewModel.swift */; };
		D0000000000000000000000D /* HomeView.swift in Sources */ = {isa = PBXBuildFile; fileRef = C0000000000000000000000D /* HomeView.swift */; };
		D0000000000000000000000E /* PreviewView.swift in Sources */ = {isa = PBXBuildFile; fileRef = C0000000000000000000000E /* PreviewView.swift */; };
		D0000000000000000000000F /* ResultsView.swift in Sources */ = {isa = PBXBuildFile; fileRef = C0000000000000000000000F /* ResultsView.swift */; };
		D00000000000000000000010 /* ScanningView.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000010 /* ScanningView.swift */; };
		D00000000000000000000011 /* BottomActionBarView.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000011 /* BottomActionBarView.swift */; };
		D00000000000000000000012 /* DrawerView.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000012 /* DrawerView.swift */; };
		D00000000000000000000013 /* LaserScannerView.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000013 /* LaserScannerView.swift */; };
		D00000000000000000000014 /* PhotoCardView.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000014 /* PhotoCardView.swift */; };
		D00000000000000000000015 /* SetCardView.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000015 /* SetCardView.swift */; };
		D00000000000000000000016 /* StorageDonutChartView.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000016 /* StorageDonutChartView.swift */; };
		D00000000000000000000017 /* Assets.xcassets in Resources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000017 /* Assets.xcassets */; };
		D00000000000000000000018 /* DuplicateEngineTests.swift in Sources */ = {isa = PBXBuildFile; fileRef = C00000000000000000000018 /* DuplicateEngineTests.swift */; };
/* End PBXBuildFile section */

/* Begin PBXContainerItemProxy section */
		E00000000000000000000099 /* PBXContainerItemProxy */ = {
			isa = PBXContainerItemProxy;
			containerPortal = A00000000000000000000001 /* Project object */;
			proxyType = 1;
			remoteGlobalIDString = A10000000000000000000001;
			remoteInfo = PhotosRemover;
		};
/* End PBXContainerItemProxy section */

/* Begin PBXFileReference section */
		C00000000000000000000001 /* PhotosRemoverApp.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = PhotosRemoverApp.swift; sourceTree = "<group>"; };
		C00000000000000000000002 /* DuplicateSet.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = DuplicateSet.swift; sourceTree = "<group>"; };
		C00000000000000000000003 /* PhotoItem.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = PhotoItem.swift; sourceTree = "<group>"; };
		C00000000000000000000004 /* ScanProgress.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = ScanProgress.swift; sourceTree = "<group>"; };
		C00000000000000000000005 /* SimilarSensitivity.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = SimilarSensitivity.swift; sourceTree = "<group>"; };
		C00000000000000000000006 /* StorageBreakdown.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = StorageBreakdown.swift; sourceTree = "<group>"; };
		C00000000000000000000007 /* CleanedStatsStore.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = CleanedStatsStore.swift; sourceTree = "<group>"; };
		C00000000000000000000008 /* DeviceStorageService.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = DeviceStorageService.swift; sourceTree = "<group>"; };
		C00000000000000000000009 /* DuplicateEngine.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = DuplicateEngine.swift; sourceTree = "<group>"; };
		C0000000000000000000000A /* ExclusionStore.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = ExclusionStore.swift; sourceTree = "<group>"; };
		C0000000000000000000000B /* PhotoLibraryService.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = PhotoLibraryService.swift; sourceTree = "<group>"; };
		C0000000000000000000000C /* MainViewModel.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = MainViewModel.swift; sourceTree = "<group>"; };
		C0000000000000000000000D /* HomeView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = HomeView.swift; sourceTree = "<group>"; };
		C0000000000000000000000E /* PreviewView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = PreviewView.swift; sourceTree = "<group>"; };
		C0000000000000000000000F /* ResultsView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = ResultsView.swift; sourceTree = "<group>"; };
		C00000000000000000000010 /* ScanningView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = ScanningView.swift; sourceTree = "<group>"; };
		C00000000000000000000011 /* BottomActionBarView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = BottomActionBarView.swift; sourceTree = "<group>"; };
		C00000000000000000000012 /* DrawerView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = DrawerView.swift; sourceTree = "<group>"; };
		C00000000000000000000013 /* LaserScannerView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = LaserScannerView.swift; sourceTree = "<group>"; };
		C00000000000000000000014 /* PhotoCardView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = PhotoCardView.swift; sourceTree = "<group>"; };
		C00000000000000000000015 /* SetCardView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = SetCardView.swift; sourceTree = "<group>"; };
		C00000000000000000000016 /* StorageDonutChartView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = StorageDonutChartView.swift; sourceTree = "<group>"; };
		C00000000000000000000017 /* Assets.xcassets */ = {isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = Assets.xcassets; sourceTree = "<group>"; };
		C00000000000000000000018 /* DuplicateEngineTests.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = DuplicateEngineTests.swift; sourceTree = "<group>"; };
		C00000000000000000000019 /* Info.plist */ = {isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; };
		C00000000000000000000020 /* Remo.app */ = {isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = Remo.app; sourceTree = BUILT_PRODUCTS_DIR; };
		C00000000000000000000021 /* PhotosRemoverTests.xctest */ = {isa = PBXFileReference; explicitFileType = wrapper.cfbundle; includeInIndex = 0; path = PhotosRemoverTests.xctest; sourceTree = BUILT_PRODUCTS_DIR; };
/* End PBXFileReference section */

/* Begin PBXFrameworksBuildPhase section */
		E00000000000000000000001 /* Frameworks */ = {
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
		E00000000000000000000002 /* Frameworks */ = {
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
		A00000000000000000000002 /* Main Group */ = {
			isa = PBXGroup;
			children = (
				B00000000000000000000001 /* PhotosRemover */,
				B00000000000000000000007 /* PhotosRemoverTests */,
				A00000000000000000000003 /* Products */,
			);
			sourceTree = "<group>";
		};
		A00000000000000000000003 /* Products */ = {
			isa = PBXGroup;
			children = (
				C00000000000000000000020 /* Remo.app */,
				C00000000000000000000021 /* PhotosRemoverTests.xctest */,
			);
			name = Products;
			sourceTree = "<group>";
		};
		B00000000000000000000001 /* PhotosRemover */ = {
			isa = PBXGroup;
			children = (
				B00000000000000000000002 /* App */,
				B00000000000000000000003 /* Models */,
				B00000000000000000000004 /* Services */,
				B00000000000000000000005 /* ViewModels */,
				B00000000000000000000006 /* Views */,
				C00000000000000000000017 /* Assets.xcassets */,
			);
			path = PhotosRemover;
			sourceTree = "<group>";
		};
		B00000000000000000000002 /* App */ = {
			isa = PBXGroup;
			children = (
				C00000000000000000000001 /* PhotosRemoverApp.swift */,
				C00000000000000000000019 /* Info.plist */,
			);
			path = App;
			sourceTree = "<group>";
		};
		B00000000000000000000003 /* Models */ = {
			isa = PBXGroup;
			children = (
				C00000000000000000000002 /* DuplicateSet.swift */,
				C00000000000000000000003 /* PhotoItem.swift */,
				C00000000000000000000004 /* ScanProgress.swift */,
				C00000000000000000000005 /* SimilarSensitivity.swift */,
				C00000000000000000000006 /* StorageBreakdown.swift */,
			);
			path = Models;
			sourceTree = "<group>";
		};
		B00000000000000000000004 /* Services */ = {
			isa = PBXGroup;
			children = (
				C00000000000000000000007 /* CleanedStatsStore.swift */,
				C00000000000000000000008 /* DeviceStorageService.swift */,
				C00000000000000000000009 /* DuplicateEngine.swift */,
				C0000000000000000000000A /* ExclusionStore.swift */,
				C0000000000000000000000B /* PhotoLibraryService.swift */,
			);
			path = Services;
			sourceTree = "<group>";
		};
		B00000000000000000000005 /* ViewModels */ = {
			isa = PBXGroup;
			children = (
				C0000000000000000000000C /* MainViewModel.swift */,
			);
			path = ViewModels;
			sourceTree = "<group>";
		};
		B00000000000000000000006 /* Views */ = {
			isa = PBXGroup;
			children = (
				C0000000000000000000000D /* HomeView.swift */,
				C0000000000000000000000E /* PreviewView.swift */,
				C0000000000000000000000F /* ResultsView.swift */,
				C00000000000000000000010 /* ScanningView.swift */,
				B00000000000000000000008 /* Components */,
			);
			path = Views;
			sourceTree = "<group>";
		};
		B00000000000000000000008 /* Components */ = {
			isa = PBXGroup;
			children = (
				C00000000000000000000011 /* BottomActionBarView.swift */,
				C00000000000000000000012 /* DrawerView.swift */,
				C00000000000000000000013 /* LaserScannerView.swift */,
				C00000000000000000000014 /* PhotoCardView.swift */,
				C00000000000000000000015 /* SetCardView.swift */,
				C00000000000000000000016 /* StorageDonutChartView.swift */,
			);
			path = Components;
			sourceTree = "<group>";
		};
		B00000000000000000000007 /* PhotosRemoverTests */ = {
			isa = PBXGroup;
			children = (
				C00000000000000000000018 /* DuplicateEngineTests.swift */,
			);
			path = PhotosRemoverTests;
			sourceTree = "<group>";
		};
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
		A10000000000000000000001 /* PhotosRemover */ = {
			isa = PBXNativeTarget;
			buildConfigurationList = F00000000000000000000002 /* Build configuration list for PBXNativeTarget "PhotosRemover" */;
			buildPhases = (
				E00000000000000000000010 /* Sources */,
				E00000000000000000000001 /* Frameworks */,
				E00000000000000000000020 /* Resources */,
			);
			buildRules = (
			);
			dependencies = (
			);
			name = PhotosRemover;
			productName = Remo;
			productReference = C00000000000000000000020 /* Remo.app */;
			productType = "com.apple.product-type.application";
		};
		A10000000000000000000002 /* PhotosRemoverTests */ = {
			isa = PBXNativeTarget;
			buildConfigurationList = F00000000000000000000003 /* Build configuration list for PBXNativeTarget "PhotosRemoverTests" */;
			buildPhases = (
				E00000000000000000000011 /* Sources */,
				E00000000000000000000002 /* Frameworks */,
			);
			buildRules = (
			);
			dependencies = (
				A10000000000000000000098 /* PBXTargetDependency */,
			);
			name = PhotosRemoverTests;
			productName = PhotosRemoverTests;
			productReference = C00000000000000000000021 /* PhotosRemoverTests.xctest */;
			productType = "com.apple.product-type.bundle.unit-test";
		};
/* End PBXNativeTarget section */

/* Begin PBXProject section */
		A00000000000000000000001 /* Project object */ = {
			isa = PBXProject;
			attributes = {
				BuildIndependentTargetsInParallel = 1;
				LastSwiftUpdateCheck = 1500;
				LastUpgradeCheck = 1500;
				TargetAttributes = {
					A10000000000000000000001 = {
						CreatedOnToolsVersion = 15.0;
					};
					A10000000000000000000002 = {
						CreatedOnToolsVersion = 15.0;
						TestTargetID = A10000000000000000000001;
					};
				};
			};
			buildConfigurationList = F00000000000000000000001 /* Build configuration list for PBXProject "PhotosRemover" */;
			compatibilityVersion = "Xcode 14.0";
			developmentRegion = en;
			hasScannedForEncodings = 0;
			knownRegions = (
				en,
				Base,
			);
			mainGroup = A00000000000000000000002 /* Main Group */;
			productRefGroup = A00000000000000000000003 /* Products */;
			projectDirPath = "";
			projectRoot = "";
			targets = (
				A10000000000000000000001 /* PhotosRemover */,
				A10000000000000000000002 /* PhotosRemoverTests */,
			);
		};
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
		E00000000000000000000020 /* Resources */ = {
			isa = PBXResourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				D00000000000000000000017 /* Assets.xcassets in Resources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXResourcesBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
		E00000000000000000000010 /* Sources */ = {
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				D00000000000000000000001 /* PhotosRemoverApp.swift in Sources */,
				D00000000000000000000002 /* DuplicateSet.swift in Sources */,
				D00000000000000000000003 /* PhotoItem.swift in Sources */,
				D00000000000000000000004 /* ScanProgress.swift in Sources */,
				D00000000000000000000005 /* SimilarSensitivity.swift in Sources */,
				D00000000000000000000006 /* StorageBreakdown.swift in Sources */,
				D00000000000000000000007 /* CleanedStatsStore.swift in Sources */,
				D00000000000000000000008 /* DeviceStorageService.swift in Sources */,
				D00000000000000000000009 /* DuplicateEngine.swift in Sources */,
				D0000000000000000000000A /* ExclusionStore.swift in Sources */,
				D0000000000000000000000B /* PhotoLibraryService.swift in Sources */,
				D0000000000000000000000C /* MainViewModel.swift in Sources */,
				D0000000000000000000000D /* HomeView.swift in Sources */,
				D0000000000000000000000E /* PreviewView.swift in Sources */,
				D0000000000000000000000F /* ResultsView.swift in Sources */,
				D00000000000000000000010 /* ScanningView.swift in Sources */,
				D00000000000000000000011 /* BottomActionBarView.swift in Sources */,
				D00000000000000000000012 /* DrawerView.swift in Sources */,
				D00000000000000000000013 /* LaserScannerView.swift in Sources */,
				D00000000000000000000014 /* PhotoCardView.swift in Sources */,
				D00000000000000000000015 /* SetCardView.swift in Sources */,
				D00000000000000000000016 /* StorageDonutChartView.swift in Sources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
		E00000000000000000000011 /* Sources */ = {
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				D00000000000000000000018 /* DuplicateEngineTests.swift in Sources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXSourcesBuildPhase section */

/* Begin PBXTargetDependency section */
		A10000000000000000000098 /* PBXTargetDependency */ = {
			isa = PBXTargetDependency;
			target = A10000000000000000000001 /* PhotosRemover */;
			targetProxy = E00000000000000000000099 /* PBXContainerItemProxy */;
		};
/* End PBXTargetDependency section */

/* Begin XCBuildConfiguration section */
		F00000000000000000000011 /* Debug */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				CLANG_ENABLE_OBJC_WEAK = YES;
				CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;
				CLANG_WARN_BOOL_CONVERSION = YES;
				CLANG_WARN_COMMA = YES;
				CLANG_WARN_CONSTANT_CONVERSION = YES;
				CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;
				CLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR;
				CLANG_WARN_DOCUMENTATION_COMMENTS = YES;
				CLANG_WARN_EMPTY_BODY = YES;
				CLANG_WARN_ENUM_CONVERSION = YES;
				CLANG_WARN_INFINITE_RECURSION = YES;
				CLANG_WARN_INT_CONVERSION = YES;
				CLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;
				CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;
				CLANG_WARN_OBJC_LITERAL_CONVERSION = YES;
				CLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR;
				CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER = YES;
				CLANG_WARN_RANGE_LOOP_ANALYSIS = YES;
				CLANG_WARN_STRICT_PROTOTYPES = YES;
				CLANG_WARN_SUSPICIOUS_MOVE = YES;
				CLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;
				CLANG_WARN_UNREACHABLE_CODE = YES;
				CLANG_WARN__DUPLICATE_METHOD_MATCH = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = dwarf;
				ENABLE_BITCODE = NO;
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				ENABLE_TESTABILITY = YES;
				GCC_C_LANGUAGE_STANDARD = gnu17;
				GCC_DYNAMIC_NO_PIC = NO;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_OPTIMIZATION_LEVEL = 0;
				GCC_PREPROCESSOR_DEFINITIONS = (
					"DEBUG=1",
					"$(inherited)",
				);
				GCC_WARN_64_TO_32_BIT_CONVERSION = YES;
				GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
				GCC_WARN_UNDEFINED_MACROS = YES;
				GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
				GCC_WARN_UNUSED_FUNCTION = YES;
				GCC_WARN_UNUSED_VARIABLE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				MTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;
				MTL_FAST_MATH = YES;
				ONLY_ACTIVE_ARCH = YES;
				SDKROOT = iphoneos;
				SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG;
				SWIFT_OPTIMIZATION_LEVEL = "-Onone";
				SWIFT_VERSION = 5.0;
			};
			name = Debug;
		};
		F00000000000000000000012 /* Release */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				CLANG_ENABLE_OBJC_WEAK = YES;
				CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;
				CLANG_WARN_BOOL_CONVERSION = YES;
				CLANG_WARN_COMMA = YES;
				CLANG_WARN_CONSTANT_CONVERSION = YES;
				CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;
				CLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR;
				CLANG_WARN_DOCUMENTATION_COMMENTS = YES;
				CLANG_WARN_EMPTY_BODY = YES;
				CLANG_WARN_ENUM_CONVERSION = YES;
				CLANG_WARN_INFINITE_RECURSION = YES;
				CLANG_WARN_INT_CONVERSION = YES;
				CLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;
				CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;
				CLANG_WARN_OBJC_LITERAL_CONVERSION = YES;
				CLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR;
				CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER = YES;
				CLANG_WARN_RANGE_LOOP_ANALYSIS = YES;
				CLANG_WARN_STRICT_PROTOTYPES = YES;
				CLANG_WARN_SUSPICIOUS_MOVE = YES;
				CLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;
				CLANG_WARN_UNREACHABLE_CODE = YES;
				CLANG_WARN__DUPLICATE_METHOD_MATCH = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
				ENABLE_BITCODE = NO;
				ENABLE_NS_ASSERTIONS = NO;
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				GCC_C_LANGUAGE_STANDARD = gnu17;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_WARN_64_TO_32_BIT_CONVERSION = YES;
				GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
				GCC_WARN_UNDEFINED_MACROS = YES;
				GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
				GCC_WARN_UNUSED_FUNCTION = YES;
				GCC_WARN_UNUSED_VARIABLE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				MTL_ENABLE_DEBUG_INFO = NO;
				MTL_FAST_MATH = YES;
				SDKROOT = iphoneos;
				SWIFT_COMPILATION_MODE = wholemodule;
				SWIFT_OPTIMIZATION_LEVEL = "-O";
				SWIFT_VERSION = 5.0;
				VALIDATE_PRODUCT = YES;
			};
			name = Release;
		};
		F00000000000000000000021 /* Debug */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;
				CODE_SIGN_STYLE = Automatic;
				CURRENT_PROJECT_VERSION = 1;
				DEVELOPMENT_TEAM = "";
				ENABLE_PREVIEWS = YES;
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = PhotosRemover/App/Info.plist;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				MARKETING_VERSION = 1.0.0;
				PRODUCT_BUNDLE_IDENTIFIER = com.cleanpix.photosremover;
				PRODUCT_NAME = Remo;
				PRODUCT_MODULE_NAME = PhotosRemover;
				SWIFT_EMIT_LOC_STRINGS = YES;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			};
			name = Debug;
		};
		F00000000000000000000022 /* Release */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;
				CODE_SIGN_STYLE = Automatic;
				CURRENT_PROJECT_VERSION = 1;
				DEVELOPMENT_TEAM = "";
				ENABLE_PREVIEWS = YES;
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = PhotosRemover/App/Info.plist;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				MARKETING_VERSION = 1.0.0;
				PRODUCT_BUNDLE_IDENTIFIER = com.cleanpix.photosremover;
				PRODUCT_NAME = Remo;
				PRODUCT_MODULE_NAME = PhotosRemover;
				SWIFT_EMIT_LOC_STRINGS = YES;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			};
			name = Release;
		};
		F00000000000000000000031 /* Debug */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES = YES;
				BUNDLE_LOADER = "$(TEST_HOST)";
				CODE_SIGN_STYLE = Automatic;
				CURRENT_PROJECT_VERSION = 1;
				GENERATE_INFOPLIST_FILE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				MARKETING_VERSION = 1.0.0;
				PRODUCT_BUNDLE_IDENTIFIER = com.cleanpix.photosremoverTests;
				PRODUCT_NAME = "$(TARGET_NAME)";
				SWIFT_EMIT_LOC_STRINGS = NO;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
				TEST_HOST = "$(BUILT_PRODUCTS_DIR)/Remo.app/$(BUNDLE_EXECUTABLE_FOLDER_PATH)/Remo";
			};
			name = Debug;
		};
		F00000000000000000000032 /* Release */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES = YES;
				BUNDLE_LOADER = "$(TEST_HOST)";
				CODE_SIGN_STYLE = Automatic;
				CURRENT_PROJECT_VERSION = 1;
				GENERATE_INFOPLIST_FILE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				MARKETING_VERSION = 1.0.0;
				PRODUCT_BUNDLE_IDENTIFIER = com.cleanpix.photosremoverTests;
				PRODUCT_NAME = "$(TARGET_NAME)";
				SWIFT_EMIT_LOC_STRINGS = NO;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
				TEST_HOST = "$(BUILT_PRODUCTS_DIR)/Remo.app/$(BUNDLE_EXECUTABLE_FOLDER_PATH)/Remo";
			};
			name = Release;
		};
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
		F00000000000000000000001 /* Build configuration list for PBXProject "PhotosRemover" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				F00000000000000000000011 /* Debug */,
				F00000000000000000000012 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
		F00000000000000000000002 /* Build configuration list for PBXNativeTarget "PhotosRemover" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				F00000000000000000000021 /* Debug */,
				F00000000000000000000022 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
		F00000000000000000000003 /* Build configuration list for PBXNativeTarget "PhotosRemoverTests" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				F00000000000000000000031 /* Debug */,
				F00000000000000000000032 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
/* End XCConfigurationList section */

	};
	rootObject = A00000000000000000000001 /* Project object */;
}
"""
    with open(os.path.join(xcodeproj_dir, "project.pbxproj"), "w", encoding="utf-8") as f:
        f.write(pbxproj_content)

    print("Successfully generated PhotosRemover.xcodeproj!")

if __name__ == "__main__":
    target_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    generate_xcodeproj(target_dir)
