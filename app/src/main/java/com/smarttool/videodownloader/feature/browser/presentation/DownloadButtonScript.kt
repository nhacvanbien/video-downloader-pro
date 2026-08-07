package com.smarttool.videodownloader.feature.browser.presentation

/**
 * Injected into every finished page: overlays a 📥 button on each image large enough
 * to be worth saving, and keeps doing so for images that appear while scrolling.
 * Taps call back through `window.Android` — see [WebAppInterface].
 */
internal const val DOWNLOAD_BUTTON_SCRIPT = """
(function() {
    function isValidImage(img) {
        let imageUrl = img.src || img.style.backgroundImage.replace(/url\(["']?|["']?\)/g, "");

        // Bỏ qua ảnh từ static.xx.fbcdn.net (icon, emoji)
        if (imageUrl.includes("static.xx.fbcdn.net")) {
            return false;
        }

        // Bỏ qua ảnh nhỏ hơn 100x100 (mức tương đối để lọc icon)
        if (img.naturalWidth < 100 || img.naturalHeight < 100) {
            return false;
        }

        return true;
    }

    function addDownloadButton(img) {
        if (!isValidImage(img) || img.dataset.downloadAdded) return;
        img.dataset.downloadAdded = true;

        let btn = document.createElement('button');
        btn.innerText = '📥';
        btn.style.position = 'absolute';
        btn.style.top = '10px';
        btn.style.right = '10px';
        btn.style.zIndex = '9999';
        btn.style.background = 'rgba(0,0,0,0.7)';
        btn.style.color = 'white';
        btn.style.border = 'none';
        btn.style.padding = '6px 11px';
        btn.style.cursor = 'pointer';
        btn.style.fontSize = '17px';
        btn.style.pointerEvents = 'auto';
        btn.style.borderRadius = '5px';

        btn.addEventListener('click', function(event) {
            event.stopPropagation();
            event.preventDefault();
            window.Android.downloadImageUpdate(img.src);
        });

        let parent = img.closest('div[role="img"]') || img.parentNode;
        if (parent) {
            parent.style.position = 'relative';
            parent.appendChild(btn);
        }
    }

    function scanImages() {
        document.querySelectorAll('div[role="img"], img').forEach(img => {
            addDownloadButton(img);
        });
    }

    scanImages();

    // Theo dõi ảnh mới xuất hiện khi cuộn trang
    let observer = new MutationObserver((mutations) => {
        mutations.forEach(mutation => {
            mutation.addedNodes.forEach(node => {
                if (node.nodeType === 1) {
                    let imgs = node.querySelectorAll('div[role="img"], img');
                    imgs.forEach(img => addDownloadButton(img));
                }
            });
        });
    });

    observer.observe(document.body, { childList: true, subtree: true });
})();
"""
