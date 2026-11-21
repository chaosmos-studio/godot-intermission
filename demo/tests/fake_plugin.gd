extends RefCounted

## Stands in for the Android singleton: the same method names and the same signals, with the same plain arguments
## the Kotlin side sends.

signal initialized(ok: bool, error: String, code: int, message: String)
signal ad_loaded(format: String, ad_unit_id: String)
signal ad_failed_to_load(format: String, ad_unit_id: String, error: String, code: int, message: String)
signal ad_shown(format: String, ad_unit_id: String, network: String)
signal ad_failed_to_show(format: String, ad_unit_id: String, error: String, code: int, message: String)
signal ad_clicked(format: String, ad_unit_id: String)
signal ad_closed(format: String, ad_unit_id: String, rewarded: bool)
signal banner_loaded(ad_unit_id: String, width_dp: int, height_dp: int)
signal banner_failed_to_load(ad_unit_id: String, error: String, code: int, message: String)
signal app_left(format: String, ad_unit_id: String, clicked: bool)
signal ad_overlong(format: String, ad_unit_id: String, seconds: float)

var calls: PackedStringArray = []
var ready_units: PackedStringArray = []

func initialize(app_key: String, test_mode: bool) -> void:
	calls.append("initialize %s %s" % [app_key, test_mode])

func isInitialized() -> bool:
	return true

func isTestMode() -> bool:
	return true

func sdkVersion() -> String:
	return "9.8.0"

func setGdprConsent(granted: bool) -> void:
	calls.append("setGdprConsent %s" % granted)

func setUsPrivacyOptOut(opted_out: bool) -> void:
	calls.append("setUsPrivacyOptOut %s" % opted_out)

func setChildDirected(child: bool) -> void:
	calls.append("setChildDirected %s" % child)

func loadAd(format: String, ad_unit_id: String) -> void:
	calls.append("loadAd %s %s" % [format, ad_unit_id])

func isAdReady(ad_unit_id: String) -> bool:
	return ready_units.has(ad_unit_id)

func showAd(ad_unit_id: String, placement: String) -> void:
	calls.append("showAd %s %s" % [ad_unit_id, placement])

func loadBanner(ad_unit_id: String, size: String, placement: String) -> void:
	calls.append("loadBanner %s %s %s" % [ad_unit_id, size, placement])

func showBanner(anchor: String, margin_dp: float) -> void:
	calls.append("showBanner %s %s" % [anchor, margin_dp])

func hideBanner() -> void:
	calls.append("hideBanner")

func destroyBanner() -> void:
	calls.append("destroyBanner")

func setOverlongSeconds(seconds: float) -> void:
	calls.append("setOverlongSeconds %s" % seconds)

func openTestSuite() -> void:
	calls.append("openTestSuite")
