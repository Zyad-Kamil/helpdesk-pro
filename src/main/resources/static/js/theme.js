// HelpDesk Pro - theme. Loaded in <head> so that the stored choice is applied before the first paint.
(function () {
	var KEY = 'hp-theme';
	var root = document.documentElement;

	function stored() {
		try {
			var value = window.localStorage.getItem(KEY);
			return value === 'light' || value === 'dark' ? value : null;
		} catch (e) {
			return null;
		}
	}

	function current() {
		return root.getAttribute('data-bs-theme') === 'light' ? 'light' : 'dark';
	}

	function set(theme, persist) {
		root.setAttribute('data-bs-theme', theme);
		if (persist) {
			try {
				window.localStorage.setItem(KEY, theme);
			} catch (e) {
				// Storage blocked: the choice simply lasts for this page.
			}
		}
		document.dispatchEvent(new CustomEvent('hp:theme', { detail: theme }));
	}

	// Dark is the default; a saved preference wins.
	set(stored() || 'dark', false);

	window.hpTheme = {
		get: current,
		toggle: function () {
			var next = current() === 'dark' ? 'light' : 'dark';
			set(next, true);
			return next;
		}
	};
})();
