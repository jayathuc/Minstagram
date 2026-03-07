package com.jayathu.minstagram.util

object JavaScriptInjector {

    const val MOBILE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * Hides Explore, Shop, and "Suggested for you" sections from Instagram's mobile web DOM.
     * Uses a MutationObserver to re-run as Instagram's SPA updates the page.
     */
    val HIDE_DISTRACTIONS = """
        (function() {
            'use strict';

            function hideElements() {
                // Hide Explore nav item
                document.querySelectorAll('a[href="/explore/"]').forEach(function(el) {
                    var parent = el.closest('li') || el.parentElement;
                    if (parent) parent.style.setProperty('display', 'none', 'important');
                });

                // Hide Shop / Marketplace links
                document.querySelectorAll('a[href*="/shop"], a[href*="marketplace"]').forEach(function(el) {
                    var parent = el.closest('li') || el.parentElement;
                    if (parent) parent.style.setProperty('display', 'none', 'important');
                });

                // Hide "Suggested for you" post sections
                document.querySelectorAll('span').forEach(function(span) {
                    if (span.textContent.trim() === 'Suggested for you') {
                        var article = span.closest('article') || span.closest('[data-media-id]');
                        if (article) article.style.setProperty('display', 'none', 'important');
                    }
                });
            }

            hideElements();

            var observer = new MutationObserver(function() { hideElements(); });
            if (document.body) {
                observer.observe(document.body, { childList: true, subtree: true });
            } else {
                document.addEventListener('DOMContentLoaded', function() {
                    observer.observe(document.body, { childList: true, subtree: true });
                    hideElements();
                });
            }
        })();
    """.trimIndent()

    /**
     * Pauses all videos and prevents autoplay. Intercepts new video elements added to the DOM.
     */
    val DISABLE_AUTOPLAY = """
        (function() {
            'use strict';

            function pauseVideos() {
                document.querySelectorAll('video').forEach(function(v) {
                    v.autoplay = false;
                    if (!v.paused) v.pause();
                });
            }

            pauseVideos();

            var observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                    mutation.addedNodes.forEach(function(node) {
                        if (node.nodeType !== 1) return;
                        var videos = node.tagName === 'VIDEO' ? [node] : node.querySelectorAll('video');
                        videos.forEach(function(v) {
                            v.autoplay = false;
                            v.pause();
                        });
                    });
                });
            });

            if (document.body) {
                observer.observe(document.body, { childList: true, subtree: true });
            }
        })();
    """.trimIndent()

    /**
     * Blocks SPA navigation to Explore and Shop by overriding history.pushState/replaceState.
     */
    val BLOCK_EXPLORE_NAVIGATION = """
        (function() {
            'use strict';

            var blockedPaths = ['/explore', '/shop', '/marketplace'];

            function isBlocked(url) {
                if (!url) return false;
                var str = url.toString();
                return blockedPaths.some(function(path) { return str.indexOf(path) !== -1; });
            }

            var originalPushState = history.pushState.bind(history);
            var originalReplaceState = history.replaceState.bind(history);

            history.pushState = function(state, title, url) {
                if (isBlocked(url)) return;
                return originalPushState(state, title, url);
            };

            history.replaceState = function(state, title, url) {
                if (isBlocked(url)) return;
                return originalReplaceState(state, title, url);
            };
        })();
    """.trimIndent()
}
