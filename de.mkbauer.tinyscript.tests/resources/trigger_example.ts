// @trigger(ADDRESS, ON_SAVE)
function trigger (addresses) {
	foreach (var address in addresses) {
		if (address.PLZ == "76131") {
			address.city = "Karlsruhe";
			// More stuff...
		}
	}
}

