// HelpDesk Pro - interface behavior, native JavaScript only.
// Features: delete confirmation, theme toggle, responsive drawer, toasts, command palette.
(function () {
	'use strict';

	var doc = document;

	// ---------------------------------------------------------------- confirmation
	// Any form carrying data-confirm asks before it is submitted.
	doc.addEventListener('submit', function (event) {
		var message = event.target.getAttribute && event.target.getAttribute('data-confirm');
		if (message && !window.confirm(message)) {
			event.preventDefault();
		}
	});

	// ---------------------------------------------------------------- theme toggle
	function syncThemeButtons() {
		var theme = window.hpTheme ? window.hpTheme.get() : 'dark';
		var label = theme === 'dark' ? 'Passer au thème clair' : 'Passer au thème sombre';
		doc.querySelectorAll('[data-hp-theme-toggle]').forEach(function (button) {
			button.setAttribute('aria-label', label);
			button.setAttribute('title', label);
		});
	}

	doc.querySelectorAll('[data-hp-theme-toggle]').forEach(function (button) {
		button.addEventListener('click', function () {
			if (window.hpTheme) {
				window.hpTheme.toggle();
			}
		});
	});
	doc.addEventListener('hp:theme', syncThemeButtons);
	syncThemeButtons();

	// ---------------------------------------------------------------- responsive drawer
	var sidebar = doc.getElementById('hp-sidebar');
	var backdrop = doc.getElementById('hp-backdrop');
	var drawerButton = doc.querySelector('[data-hp-drawer-open]');
	var desktop = window.matchMedia('(min-width: 992px)');

	function setDrawer(open) {
		if (!sidebar) {
			return;
		}
		sidebar.classList.toggle('is-open', open);
		if (backdrop) {
			backdrop.classList.toggle('is-open', open);
		}
		if (drawerButton) {
			drawerButton.setAttribute('aria-expanded', String(open));
		}
		if (open) {
			var first = sidebar.querySelector('a');
			if (first) {
				first.focus();
			}
		} else if (drawerButton && doc.activeElement && sidebar.contains(doc.activeElement)) {
			drawerButton.focus();
		}
	}

	if (drawerButton) {
		drawerButton.addEventListener('click', function () {
			setDrawer(!sidebar.classList.contains('is-open'));
		});
	}
	if (backdrop) {
		backdrop.addEventListener('click', function () {
			setDrawer(false);
		});
	}
	if (sidebar) {
		sidebar.addEventListener('click', function (event) {
			if (event.target.closest('a')) {
				setDrawer(false);
			}
		});
	}
	doc.addEventListener('keydown', function (event) {
		if (event.key === 'Escape' && sidebar && sidebar.classList.contains('is-open')) {
			setDrawer(false);
		}
	});
	desktop.addEventListener('change', function (event) {
		if (event.matches) {
			setDrawer(false);
		}
	});

	// ---------------------------------------------------------------- toasts
	// The server renders each flash message as a hidden .hp-flash element. It is turned into a toast
	// here, so that assistive technologies announce it (the container is a live region).
	var toastBox = doc.getElementById('hp-toasts');

	function closeToast(toast) {
		if (!toast.parentNode) {
			return;
		}
		toast.classList.add('is-leaving');
		window.setTimeout(function () {
			if (toast.parentNode) {
				toast.parentNode.removeChild(toast);
			}
		}, 280);
	}

	function showToast(message, kind) {
		if (!toastBox) {
			return;
		}
		var error = kind === 'error';
		var toast = doc.createElement('div');
		toast.className = 'hp-toast' + (error ? ' hp-toast-error' : '');
		toast.setAttribute('role', error ? 'alert' : 'status');

		var icon = doc.createElement('i');
		icon.className = 'bi ' + (error ? 'bi-exclamation-octagon-fill' : 'bi-check-circle-fill');
		icon.setAttribute('aria-hidden', 'true');

		var body = doc.createElement('div');
		body.className = 'hp-toast-body';
		body.textContent = message;

		var close = doc.createElement('button');
		close.type = 'button';
		close.className = 'hp-toast-close';
		close.setAttribute('aria-label', 'Fermer la notification');
		close.innerHTML = '<i class="bi bi-x-lg" aria-hidden="true"></i>';
		close.addEventListener('click', function () {
			closeToast(toast);
		});

		toast.appendChild(icon);
		toast.appendChild(body);
		toast.appendChild(close);
		toastBox.appendChild(toast);

		// Confirmations fade away unless hovered or focused; errors stay until closed.
		if (!error) {
			var timer = window.setTimeout(function () {
				closeToast(toast);
			}, 6500);
			var hold = function () {
				window.clearTimeout(timer);
			};
			toast.addEventListener('mouseenter', hold);
			toast.addEventListener('focusin', hold);
		}
	}

	doc.querySelectorAll('.hp-flash').forEach(function (flash) {
		showToast(flash.textContent.trim(), flash.getAttribute('data-kind'));
		flash.parentNode.removeChild(flash);
	});

	// ---------------------------------------------------------------- command palette
	var palette = doc.getElementById('hp-palette');
	var input = doc.getElementById('hp-palette-input');
	var list = doc.getElementById('hp-palette-list');

	if (palette && input && list && typeof palette.showModal === 'function') {
		var items = Array.prototype.slice.call(list.querySelectorAll('.hp-palette-item'));
		var groups = Array.prototype.slice.call(list.querySelectorAll('.hp-palette-group'));
		var empty = doc.getElementById('hp-palette-empty');
		var searchItem = list.querySelector('[data-palette-search]');
		var gotoItem = list.querySelector('[data-palette-goto]');
		var activeIndex = 0;

		var fold = function (text) {
			return text.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
		};

		var visibleItems = function () {
			return items.filter(function (item) {
				return !item.hidden;
			});
		};

		var setActive = function (index) {
			var visible = visibleItems();
			items.forEach(function (item) {
				item.classList.remove('is-active');
				item.removeAttribute('aria-selected');
			});
			if (!visible.length) {
				input.removeAttribute('aria-activedescendant');
				return;
			}
			activeIndex = (index + visible.length) % visible.length;
			var item = visible[activeIndex];
			item.classList.add('is-active');
			item.setAttribute('aria-selected', 'true');
			input.setAttribute('aria-activedescendant', item.id);
			item.scrollIntoView({ block: 'nearest' });
		};

		var filter = function () {
			var raw = input.value.trim();
			var query = fold(raw);
			items.forEach(function (item) {
				if (item === searchItem || item === gotoItem) {
					return;
				}
				item.hidden = query !== '' && fold(item.getAttribute('data-label') || '').indexOf(query) === -1;
			});
			// Dynamic entries only exist when they lead somewhere real.
			if (searchItem) {
				searchItem.hidden = raw === '';
				if (raw !== '') {
					searchItem.querySelector('a').href = searchItem.getAttribute('data-base') + encodeURIComponent(raw);
					searchItem.querySelector('.hp-palette-query').textContent = raw;
				}
			}
			if (gotoItem) {
				var number = /^#?(\d{1,9})$/.exec(raw);
				gotoItem.hidden = !number;
				if (number) {
					gotoItem.querySelector('a').href = gotoItem.getAttribute('data-base') + number[1];
					gotoItem.querySelector('.hp-palette-number').textContent = number[1];
				}
			}
			groups.forEach(function (group) {
				var next = group.nextElementSibling;
				var any = false;
				while (next && !next.classList.contains('hp-palette-group')) {
					if (next.classList.contains('hp-palette-item') && !next.hidden) {
						any = true;
					}
					next = next.nextElementSibling;
				}
				group.hidden = !any;
			});
			if (empty) {
				empty.hidden = visibleItems().length > 0;
			}
			setActive(0);
		};

		var run = function (item) {
			if (!item) {
				return;
			}
			var action = item.querySelector('[data-palette-action]');
			if (action && action.getAttribute('data-palette-action') === 'theme') {
				palette.close();
				if (window.hpTheme) {
					window.hpTheme.toggle();
				}
				return;
			}
			var link = item.querySelector('a');
			if (link) {
				palette.close();
				window.location.href = link.href;
			}
		};

		var openPalette = function () {
			if (palette.open) {
				return;
			}
			input.value = '';
			palette.showModal();
			filter();
			input.focus();
		};

		doc.querySelectorAll('[data-hp-palette-open]').forEach(function (button) {
			button.addEventListener('click', openPalette);
		});
		// Ctrl+K (Cmd+K on macOS) opens the palette from any page.
		doc.addEventListener('keydown', function (event) {
			if ((event.ctrlKey || event.metaKey) && !event.altKey && event.key.toLowerCase() === 'k') {
				event.preventDefault();
				if (palette.open) {
					palette.close();
				} else {
					openPalette();
				}
			}
		});
		input.addEventListener('input', filter);
		input.addEventListener('keydown', function (event) {
			if (event.key === 'ArrowDown') {
				event.preventDefault();
				setActive(activeIndex + 1);
			} else if (event.key === 'ArrowUp') {
				event.preventDefault();
				setActive(activeIndex - 1);
			} else if (event.key === 'Enter') {
				event.preventDefault();
				run(visibleItems()[activeIndex]);
			}
		});
		list.addEventListener('click', function (event) {
			var item = event.target.closest('.hp-palette-item');
			if (item) {
				event.preventDefault();
				run(item);
			}
		});
		list.addEventListener('mousemove', function (event) {
			var item = event.target.closest('.hp-palette-item');
			var visible = visibleItems();
			if (item && visible.indexOf(item) !== -1 && visible.indexOf(item) !== activeIndex) {
				setActive(visible.indexOf(item));
			}
		});
		// A click on the backdrop (outside the dialog box) closes it.
		palette.addEventListener('click', function (event) {
			if (event.target === palette) {
				palette.close();
			}
		});
	} else {
		// Without <dialog> support, the buttons are of no use: hide them rather than leave dead controls.
		doc.querySelectorAll('[data-hp-palette-open]').forEach(function (button) {
			button.hidden = true;
		});
	}
})();
