extends Control

## One screen to try every call on a phone, with a log of every signal. It doubles as the device test harness.

## LevelPlay's public demo app, so the demo shows ads with nothing set up. Test mode uses it anyway.
const DEMO_APP_KEY: String = "25b63cf85"
const DEMO_INTERSTITIAL: String = "h3xw38h9214adgxo"
const DEMO_REWARDED: String = "syz3d8ekts22q0or"
const DEMO_BANNER: String = "4fpetq4lhe5lsw3e"
const FONT_SIZE: int = 30
const LOG_LINES: int = 14

var _ads: Intermission
var _app_key: LineEdit
var _interstitial: LineEdit
var _rewarded: LineEdit
var _banner: LineEdit
var _test_mode: CheckButton
var _adaptive: CheckButton
var _status: Label
var _log: Label
var _lines: PackedStringArray = []
var _rewards: int = 0

func _ready() -> void:
	_ads = Intermission.new()
	add_child(_ads)
	_listen()
	_build()
	_say("available %s, provider \"%s\", sdk \"%s\"" % [_ads.available(), _ads.provider_name(), _ads.sdk_version()])

func _build() -> void:
	theme = Theme.new()
	theme.default_font_size = FONT_SIZE
	var margin: MarginContainer = MarginContainer.new()
	margin.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	for side: String in ["left", "right", "top", "bottom"]:
		margin.add_theme_constant_override("margin_" + side, 24)
	add_child(margin)
	var scroll: ScrollContainer = ScrollContainer.new()
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	margin.add_child(scroll)
	var column: VBoxContainer = VBoxContainer.new()
	column.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	column.add_theme_constant_override("separation", 12)
	scroll.add_child(column)

	_status = Label.new()
	_status.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	column.add_child(_status)
	_app_key = _field(column, "App key", DEMO_APP_KEY)
	_interstitial = _field(column, "Interstitial", DEMO_INTERSTITIAL)
	_rewarded = _field(column, "Rewarded", DEMO_REWARDED)
	_banner = _field(column, "Banner", DEMO_BANNER)
	_test_mode = _toggle(column, "Test mode", true)
	var gdpr: CheckButton = _toggle(column, "Consent given (GDPR)", false)
	gdpr.toggled.connect(func(on: bool) -> void:
		_ads.set_gdpr_consent(on)
		_say("gdpr consent %s" % on))
	var opt_out: CheckButton = _toggle(column, "Do not sell (US)", false)
	opt_out.toggled.connect(func(on: bool) -> void:
		_ads.set_us_privacy_opt_out(on)
		_say("us opt-out %s" % on))
	_row(column, [["Initialize", _initialize], ["Test suite", _ads.open_test_suite]])
	_row(column, [["Load interstitial", func() -> void: _ads.load_ad(Intermission.Format.INTERSTITIAL, _interstitial.text)],
		["Show", func() -> void: _ads.show_ad(_interstitial.text, "demo")]])
	_row(column, [["Load rewarded", func() -> void: _ads.load_ad(Intermission.Format.REWARDED, _rewarded.text)],
		["Show", func() -> void: _ads.show_ad(_rewarded.text, "demo")]])
	_adaptive = _toggle(column, "Adaptive banner", false)
	_row(column, [["Load banner", _load_banner], ["Destroy", _ads.destroy_banner]])
	_row(column, [["Top", func() -> void: _ads.show_banner(Intermission.Anchor.TOP, Vector2(0, 24))],
		["Bottom", func() -> void: _ads.show_banner(Intermission.Anchor.BOTTOM, Vector2(0, 48))],
		["Custom", func() -> void: _ads.show_banner(Intermission.Anchor.CUSTOM, Vector2(16, 200))],
		["Hide", _ads.hide_banner]])
	_row(column, [["Overlong after 20 s", func() -> void:
		_ads.set_overlong_seconds(20.0)
		_say("overlong after 20 s")]])
	_log = Label.new()
	_log.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_log.add_theme_font_size_override("font_size", 22)
	column.add_child(_log)
	_refresh_status()

func _field(parent: Control, label: String, value: String) -> LineEdit:
	var row: HBoxContainer = HBoxContainer.new()
	var caption: Label = Label.new()
	caption.text = label
	caption.custom_minimum_size.x = 200
	row.add_child(caption)
	var edit: LineEdit = LineEdit.new()
	edit.text = value
	edit.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	row.add_child(edit)
	parent.add_child(row)
	return edit

func _toggle(parent: Control, label: String, on: bool) -> CheckButton:
	var button: CheckButton = CheckButton.new()
	button.text = label
	button.button_pressed = on
	parent.add_child(button)
	return button

## Each entry is [label, Callable].
func _row(parent: Control, buttons: Array) -> void:
	var row: HBoxContainer = HBoxContainer.new()
	for entry: Array in buttons:
		var button: Button = Button.new()
		button.text = entry[0]
		button.custom_minimum_size.y = 88
		button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		var action: Callable = entry[1]
		button.pressed.connect(func() -> void: action.call())
		row.add_child(button)
	parent.add_child(row)

func _initialize() -> void:
	_ads.enable_test_suite()
	_ads.initialize(_app_key.text, _test_mode.button_pressed)
	_say("initialize, test mode %s" % _test_mode.button_pressed)

func _load_banner() -> void:
	var size: Intermission.BannerSize = Intermission.BannerSize.ADAPTIVE if _adaptive.button_pressed \
		else Intermission.BannerSize.BANNER
	_ads.load_banner(_banner.text, size, "demo")

func _listen() -> void:
	_ads.initialized.connect(func(ok: bool, error: Intermission.AdError, code: int, message: String) -> void:
		_say("initialized ok=%s error=%s code=%d %s" % [ok, _error(error), code, message])
		_refresh_status())
	_ads.ad_loaded.connect(func(format: Intermission.Format, unit: String) -> void:
		_say("loaded %s %s" % [_format(format), unit]))
	_ads.ad_failed_to_load.connect(func(format: Intermission.Format, unit: String, error: Intermission.AdError, code: int,
			message: String) -> void:
		_say("load failed %s %s %s %d %s" % [_format(format), unit, _error(error), code, message]))
	_ads.ad_shown.connect(func(format: Intermission.Format, unit: String, network: String) -> void:
		_say("shown %s %s from %s" % [_format(format), unit, network]))
	_ads.ad_failed_to_show.connect(func(format: Intermission.Format, unit: String, error: Intermission.AdError, code: int,
			message: String) -> void:
		_say("show failed %s %s %s %d %s" % [_format(format), unit, _error(error), code, message]))
	_ads.ad_clicked.connect(func(format: Intermission.Format, unit: String) -> void:
		_say("clicked %s %s" % [_format(format), unit]))
	_ads.ad_rewarded.connect(func(unit: String, reward: String, amount: int) -> void:
		_rewards += 1
		_say("REWARDED %s %s x%d (rewards so far %d)" % [unit, reward, amount, _rewards]))
	_ads.ad_closed.connect(func(format: Intermission.Format, unit: String, rewarded: bool) -> void:
		_say("closed %s %s rewarded=%s" % [_format(format), unit, rewarded]))
	_ads.app_left.connect(func(format: Intermission.Format, unit: String, clicked: bool) -> void:
		_say("APP LEFT %s %s clicked=%s" % [_format(format), unit, clicked]))
	_ads.ad_overlong.connect(func(format: Intermission.Format, unit: String, seconds: float) -> void:
		_say("OVERLONG %s %s %.1f s" % [_format(format), unit, seconds]))
	_ads.banner_loaded.connect(func(unit: String, size_dp: Vector2) -> void:
		_say("banner loaded %s %dx%d dp" % [unit, int(size_dp.x), int(size_dp.y)]))
	_ads.banner_failed_to_load.connect(func(unit: String, error: Intermission.AdError, code: int, message: String) -> void:
		_say("banner failed %s %s %d %s" % [unit, _error(error), code, message]))
	_ads.banner_shown.connect(func(unit: String, network: String) -> void:
		_say("banner shown %s from %s" % [unit, network]))
	_ads.banner_clicked.connect(func(unit: String) -> void: _say("banner clicked %s" % unit))

func _notification(what: int) -> void:
	if what == NOTIFICATION_APPLICATION_PAUSED:
		_say("app paused")
	elif what == NOTIFICATION_APPLICATION_RESUMED:
		_say("app resumed")

func _refresh_status() -> void:
	if _status != null:
		_status.text = "Intermission %s | %s %s | test mode %s" % [
			"ready" if _ads.is_initialized() else "not ready", _ads.provider_name(), _ads.sdk_version(), _ads.is_test_mode()]

func _say(line: String) -> void:
	print("INTERMISSION_DEMO ", line)
	_lines.append(line)
	if _lines.size() > LOG_LINES:
		_lines = _lines.slice(_lines.size() - LOG_LINES)
	if _log != null:
		_log.text = "\n".join(_lines)

static func _format(format: int) -> String:
	return "unknown" if format < 0 else String(Intermission.Format.keys()[format]).to_lower()

static func _error(error: int) -> String:
	return String(Intermission.AdError.keys()[error]).to_lower()
