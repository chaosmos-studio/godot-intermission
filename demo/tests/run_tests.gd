extends SceneTree

## godot --headless --path demo -s res://tests/run_tests.gd
## The addon's GDScript side against a fake singleton, and its agreement with the Kotlin sources and the version
## catalog, which every check reads from the repo around the demo.

const FakePlugin: GDScript = preload("res://tests/fake_plugin.gd")
const ExportPlugin: GDScript = preload("res://addons/intermission/export_plugin.gd")
const WRAPPER: String = "res://addons/intermission/intermission.gd"

var _passed: int = 0
var _failed: int = 0
## Every emission of the wrapper under test, as [signal name, args].
var _heard: Array[Array] = []

func _initialize() -> void:
	_run.call_deferred()

func _run() -> void:
	await _inert_without_the_plugin()
	_calls_reach_the_plugin_by_wire_name()
	_signals_arrive_as_enums()
	_unknown_names_stay_visible()
	_wrapper_matches_the_kotlin_side()
	_export_plugin_matches_the_version_catalog()
	_docs_match_the_api()
	print("%d passed, %d failed" % [_passed, _failed])
	quit(1 if _failed > 0 else 0)

func _check(what: String, ok: bool) -> void:
	if ok:
		_passed += 1
	else:
		_failed += 1
		printerr("FAIL: ", what)

## Signals bind their name last, after however many arguments they carry.
func _hear(a: Variant = null, b: Variant = null, c: Variant = null, d: Variant = null, e: Variant = null,
		f: Variant = null) -> void:
	var args: Array = [a, b, c, d, e, f]
	var name: String = ""
	for i: int in range(args.size() - 1, -1, -1):
		if args[i] is String and String(args[i]).begins_with("@"):
			name = String(args[i]).substr(1)
			args.resize(i)
			break
	_heard.append([name, args])

func _last(name: String) -> Array:
	for i: int in range(_heard.size() - 1, -1, -1):
		if _heard[i][0] == name:
			return _heard[i][1]
	return []

func _listen(ads: Intermission) -> void:
	_heard.clear()
	for info: Dictionary in (ads.get_script() as Script).get_script_signal_list():
		var name: String = info["name"]
		ads.connect(name, _hear.bind("@" + name))

func _inert_without_the_plugin() -> void:
	var ads: Intermission = Intermission.new()
	_listen(ads)
	_check("no singleton off Android", not ads.available())
	ads.initialize("key", true)
	ads.load_ad(Intermission.Format.REWARDED, "r1")
	ads.show_ad("i1")
	ads.load_banner("b1")
	_check("nothing answers inside the call", _heard.is_empty())
	await process_frame
	_check("initialize answers not available",
		_last("initialized").slice(0, 3) == [false, Intermission.AdError.NOT_AVAILABLE, 0])
	_check("a load answers not available, as no fill would",
		_last("ad_failed_to_load").slice(0, 4) == [Intermission.Format.REWARDED, "r1", Intermission.AdError.NOT_AVAILABLE, 0])
	_check("a show answers not available",
		_last("ad_failed_to_show").slice(0, 3) == [Intermission.UNKNOWN_FORMAT, "i1", Intermission.AdError.NOT_AVAILABLE])
	_check("a banner answers not available",
		_last("banner_failed_to_load").slice(0, 2) == ["b1", Intermission.AdError.NOT_AVAILABLE])
	_check("nothing is ready", not ads.is_ad_ready("r1") and not ads.is_initialized() and not ads.is_test_mode())
	_check("no SDK version", ads.sdk_version() == "")
	ads.show_banner()
	ads.hide_banner()
	ads.destroy_banner()
	ads.set_gdpr_consent(true)
	ads.open_test_suite()
	ads.free()

func _calls_reach_the_plugin_by_wire_name() -> void:
	var fake: RefCounted = FakePlugin.new()
	var ads: Intermission = Intermission.new(fake)
	_check("a stand-in plugin makes it available", ads.available())
	ads.initialize("key", false)
	ads.set_gdpr_consent(false)
	ads.set_us_privacy_opt_out(true)
	ads.set_child_directed(false)
	ads.load_ad(Intermission.Format.INTERSTITIAL, "i1")
	ads.load_ad(Intermission.Format.REWARDED, "r1")
	ads.show_ad("r1", "hint")
	ads.load_banner("b1", Intermission.BannerSize.ADAPTIVE, "home")
	ads.show_banner(Intermission.Anchor.TOP, 24.5)
	ads.show_banner()
	ads.hide_banner()
	ads.destroy_banner()
	ads.set_overlong_seconds(20.0)
	ads.open_test_suite()
	var calls: PackedStringArray = fake.get("calls")
	var expected: PackedStringArray = [
		"initialize key false", "setGdprConsent false", "setUsPrivacyOptOut true", "setChildDirected false",
		"loadAd interstitial i1", "loadAd rewarded r1", "showAd r1 hint", "loadBanner b1 adaptive home",
		"showBanner top 24.5", "showBanner bottom 0.0", "hideBanner", "destroyBanner", "setOverlongSeconds 20.0",
		"openTestSuite",
	]
	_check("every call reaches the plugin with its wire names (%s)" % ", ".join(calls), calls == expected)
	fake.set("ready_units", PackedStringArray(["r1"]))
	_check("readiness is the plugin's", ads.is_ad_ready("r1") and not ads.is_ad_ready("i1"))
	_check("state queries pass through", ads.is_initialized() and ads.is_test_mode() and ads.sdk_version() == "9.8.0")
	ads.free()

func _signals_arrive_as_enums() -> void:
	var fake: RefCounted = FakePlugin.new()
	var ads: Intermission = Intermission.new(fake)
	_listen(ads)
	fake.emit_signal(&"initialized", true, "none", 0, "")
	_check("initialized", _last("initialized") == [true, Intermission.AdError.NONE, 0, ""])
	fake.emit_signal(&"ad_loaded", "rewarded", "r1")
	_check("ad_loaded", _last("ad_loaded") == [Intermission.Format.REWARDED, "r1"])
	fake.emit_signal(&"ad_failed_to_load", "interstitial", "i1", "no_fill", 509, "No fill")
	_check("ad_failed_to_load",
		_last("ad_failed_to_load") == [Intermission.Format.INTERSTITIAL, "i1", Intermission.AdError.NO_FILL, 509, "No fill"])
	fake.emit_signal(&"ad_shown", "interstitial", "i1", "UnityAds")
	_check("ad_shown", _last("ad_shown") == [Intermission.Format.INTERSTITIAL, "i1", "UnityAds"])
	fake.emit_signal(&"ad_failed_to_show", "rewarded", "r1", "expired", 0, "gone")
	_check("ad_failed_to_show",
		_last("ad_failed_to_show") == [Intermission.Format.REWARDED, "r1", Intermission.AdError.EXPIRED, 0, "gone"])
	fake.emit_signal(&"ad_clicked", "rewarded", "r1")
	_check("ad_clicked", _last("ad_clicked") == [Intermission.Format.REWARDED, "r1"])
	fake.emit_signal(&"ad_closed", "rewarded", "r1", true)
	_check("ad_closed", _last("ad_closed") == [Intermission.Format.REWARDED, "r1", true])
	fake.emit_signal(&"app_left", "interstitial", "i1", false)
	_check("app_left", _last("app_left") == [Intermission.Format.INTERSTITIAL, "i1", false])
	fake.emit_signal(&"ad_overlong", "interstitial", "i1", 61.5)
	_check("ad_overlong", _last("ad_overlong") == [Intermission.Format.INTERSTITIAL, "i1", 61.5])
	fake.emit_signal(&"banner_loaded", "b1", 320, 50)
	_check("banner_loaded", _last("banner_loaded") == ["b1", Vector2(320, 50)])
	fake.emit_signal(&"banner_failed_to_load", "b1", "no_connection", 520, "offline")
	_check("banner_failed_to_load",
		_last("banner_failed_to_load") == ["b1", Intermission.AdError.NO_CONNECTION, 520, "offline"])
	ads.free()

func _unknown_names_stay_visible() -> void:
	var fake: RefCounted = FakePlugin.new()
	var ads: Intermission = Intermission.new(fake)
	_listen(ads)
	fake.emit_signal(&"ad_failed_to_show", "unknown", "x", "not_loaded", 0, "")
	_check("an ad unit never loaded has no format", _last("ad_failed_to_show")[0] == Intermission.UNKNOWN_FORMAT)
	fake.emit_signal(&"ad_failed_to_load", "rewarded", "r1", "something_new", 7, "")
	_check("an error this version does not know is the provider's", _last("ad_failed_to_load")[2] == Intermission.AdError.PROVIDER)
	ads.free()

func _wrapper_matches_the_kotlin_side() -> void:
	var kotlin: String = _repo_file("plugin/src/main/kotlin/studio/chaosmos/intermission/")
	var signals: String = _read(kotlin + "Signals.kt")
	var plugin: String = _read(kotlin + "IntermissionPlugin.kt")
	var types: String = _read(kotlin + "Types.kt")
	var wrapper: String = _read(WRAPPER)
	_check("the Kotlin sources are where the tests expect", signals != "" and plugin != "" and types != "")

	var fake_signals: Dictionary[String, int] = {}
	for info: Dictionary in FakePlugin.get_script_signal_list():
		fake_signals[String(info["name"])] = (info["args"] as Array).size()
	var declared: Dictionary[String, String] = _matches(signals, "const val (\\w+) = \"(\\w+)\"")
	var counts: Dictionary[String, String] = _matches(signals, "\\n\\s+(\\w+) to listOf\\(([^)]*)\\)")
	_check("Kotlin declares signals", declared.size() >= 11)
	var ads: Intermission = Intermission.new()
	for constant: String in declared:
		var name: String = declared[constant]
		var arity: int = counts.get(constant, "").split(",", false).size()
		_check("the wrapper has the signal %s" % name, ads.has_signal(name))
		_check("the stand-in sends %s with %d arguments as Kotlin does" % [name, arity],
			fake_signals.get(name, -1) == arity)
	_check("the stand-in sends no signal Kotlin lacks", fake_signals.size() == declared.size())
	ads.free()

	var methods: Dictionary[String, String] = _matches(plugin, "@UsedByGodot\\s+fun (\\w+)\\(")
	var called: Dictionary[String, String] = _matches(wrapper, "_plugin\\.call\\(&\"(\\w+)\"")
	_check("Kotlin exposes methods", methods.size() >= 15)
	for method: String in methods:
		_check("the wrapper calls %s" % method, called.has(method))
	for method: String in methods:
		_check("the stand-in has %s" % method, FakePlugin.new().has_method(method))
	for method: String in called:
		_check("the wrapper calls %s, which Kotlin exposes" % method, methods.has(method))

	_check("formats match", _wire_names(types, "Format") == _lower(Intermission.Format.keys()))
	_check("banner sizes match", _wire_names(types, "BannerSize") == _lower(Intermission.BannerSize.keys()))
	_check("anchors match", _wire_names(types, "Anchor") == _lower(Intermission.Anchor.keys()))
	var errors: PackedStringArray = _lower(Intermission.AdError.keys())
	for error: String in _wire_names(types, "AdError"):
		_check("the wrapper knows the error %s" % error, errors.has(error))
	var constants: Dictionary = (Intermission as GDScript).get_script_constant_map()
	var singleton: String = constants["_SINGLETON"]
	_check("the singleton's name", plugin.contains("const val NAME = \"%s\"" % singleton))
	_check("the manifest names the same plugin", _read(_repo_file("plugin/src/main/AndroidManifest.xml"))
		.contains("org.godotengine.plugin.v2.%s" % singleton))

func _export_plugin_matches_the_version_catalog() -> void:
	var catalog: String = _read(_repo_file("gradle/libs.versions.toml"))
	_check("the version catalog is where the tests expect", catalog != "")
	var versions: Dictionary[String, String] = _matches(catalog, "\\n([\\w-]+) = \"([^\"]+)\"")
	var modules: Dictionary[String, String] = _matches(catalog, "module = \"([^\"]+)\", version\\.ref = \"([\\w-]+)\"")
	var dependencies: PackedStringArray = []
	for coordinate: String in ExportPlugin.DEPENDENCIES:
		dependencies.append(coordinate.trim_suffix("!!"))
	var sdk_modules: int = 0
	for module: String in modules:
		if not (module.begins_with("com.unity3d") or module.begins_with("com.google.android.gms")):
			continue
		sdk_modules += 1
		var coordinate: String = "%s:%s" % [module, versions[modules[module]]]
		_check("the export plugin asks for %s" % coordinate, dependencies.has(coordinate))
	_check("and nothing else", dependencies.size() == sdk_modules)
	var strict: PackedStringArray = []
	for coordinate: String in ExportPlugin.DEPENDENCIES:
		if coordinate.ends_with("!!"):
			strict.append(coordinate.get_slice(":", 1))
	_check("the two libraries Unity asks for by a range are held strictly",
		strict == PackedStringArray(["adquality-sdk", "coherencelib"]))
	_check("plugin.cfg carries the catalog's version", _plugin_version() == versions["intermission"])
	_check("the library for a debug build", ExportPlugin.library(true) == "intermission/bin/debug/intermission-debug.aar")
	_check("and for a release build", ExportPlugin.library(false) == "intermission/bin/release/intermission-release.aar")

func _docs_match_the_api() -> void:
	var docs: String = _repo_file("site/src/content/docs/")
	var wrapper: String = _read(WRAPPER)
	var methods_page: String = _read(docs + "reference/methods.md")
	var signals_page: String = _read(docs + "reference/signals.md")
	_check("the reference pages are where the tests expect", methods_page != "" and signals_page != "")

	var signatures: Dictionary[String, String] = _matches(wrapper, "\\n(func ([a-z]\\w*)\\(.*\\) -> \\w+):")
	var documented: Dictionary[String, String] = _matches(methods_page, "\\n```gdscript\\n(func [^\\n]+)\\n```")
	for signature: String in signatures:
		_check("the methods page shows %s" % signature, documented.has(signature))
	_check("and no method the class lacks", documented.size() == signatures.size())

	var declared: Dictionary[String, String] = _matches(wrapper, "\\n(signal \\w+\\([^\\n]*\\))")
	var listed: Dictionary[String, String] = _matches(signals_page, "\\n```gdscript\\n(signal [^\\n]+)\\n```")
	for line: String in declared:
		_check("the signals page shows %s" % line, listed.has(line))
	_check("and no signal the class lacks", listed.size() == declared.size())

	var enums_page: String = _read(docs + "reference/enums.md")
	for error: String in Intermission.AdError.keys():
		_check("the enums page explains %s" % error, enums_page.contains("| `%s` |" % error))

	var version: String = _plugin_version()
	var install_lines: PackedStringArray = _read(docs + "install.md").split("\n")
	var pins: String = ""
	for line: String in _read(docs + "updating.md").split("\n"):
		if line.begins_with("| %s |" % version):
			pins = line
	_check("the updating page has a row for %s" % version, pins != "")
	for coordinate: String in ExportPlugin.DEPENDENCIES:
		var parts: PackedStringArray = coordinate.trim_suffix("!!").split(":")
		var row: String = ""
		for line: String in install_lines:
			if line.begins_with("| `%s:%s`" % [parts[0], parts[1]]):
				row = line
		_check("the install page pins %s at %s" % [parts[1], parts[2]], row.ends_with("| %s |" % parts[2]))
		_check("the updating page's row has %s" % parts[2], pins.contains("| %s |" % parts[2]))

func _plugin_version() -> String:
	var cfg: ConfigFile = ConfigFile.new()
	cfg.load("res://addons/intermission/plugin.cfg")
	return cfg.get_value("plugin", "version", "")

func _repo_file(path: String) -> String:
	return ProjectSettings.globalize_path("res://").path_join("..").path_join(path)

func _read(path: String) -> String:
	return FileAccess.get_file_as_string(path)

## The first group of each match, mapped to its second group (or to "" when there is none).
func _matches(text: String, pattern: String) -> Dictionary[String, String]:
	var regex: RegEx = RegEx.create_from_string(pattern)
	var found: Dictionary[String, String] = {}
	for result: RegExMatch in regex.search_all(text):
		found[result.get_string(1)] = result.get_string(2) if result.get_group_count() > 1 else ""
	return found

## The wire names of a Kotlin enum, in order.
func _wire_names(types: String, enum_name: String) -> PackedStringArray:
	var start: int = types.find("enum class %s(" % enum_name)
	var end: int = types.find("\n}", start)
	var names: PackedStringArray = []
	if start < 0:
		return names
	var regex: RegEx = RegEx.create_from_string("\\n\\s+[A-Z_]+\\(\"(\\w+)\"")
	for result: RegExMatch in regex.search_all(types.substr(start, end - start)):
		names.append(result.get_string(1))
	return names

func _lower(keys: Array) -> PackedStringArray:
	var out: PackedStringArray = []
	for key: String in keys:
		out.append(key.to_lower())
	return out
