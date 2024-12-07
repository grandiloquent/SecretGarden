

function showActions(id) {
    bottomSheetContent.innerHTML = [
        "91",
        "57",
        "收藏",
        "其他",
        "下载",
        "图片地址",
        "刷新",].map((x, k) => {
            return `<div class="menu-item" data-id="${k + 1}">
                        <button class="menu-item-button">
                            <div class="c3-icon">

                            </div>
                            <span>${x}</span>
                        </button>
                    </div>`
        }).join('');
    bottomSheetLayoutContentWrapper.style.maxHeight = 'none'
    bottomSheetContainer.style.display = 'block';
    document.querySelectorAll('.menu-item')
        .forEach(element => {
            element.addEventListener('click', evt => {
                evt.stopPropagation();
                bottomSheetContainer.style.display = 'none';
                const index = parseInt(element.dataset.id);
                switch (index) {
                    case 1:
                        moveVideo(id, 1)
                        break;
                    case 2:
                        moveVideo(id, 2)
                        break;
                    case 3:
                        moveVideo(id, 3)
                        break;
                    case 4:
                        moveVideo(id, 6)
                        break;
                    case 5:
                        NativeAndroid.downloadVideo(id)
                        break;
                    case 6:
                        const imageHost = NativeAndroid.getImageHost(id);
                        localStorage.setItem("imageHost", imageHost);
                        mImageHost=imageHost;
                        break;
                    case 7:
                        NativeAndroid.refreshVideo(id);
                        break;
                }

            })
        });
}


function moveVideo(id, videoType) {
    NativeAndroid.moveVideo(id, videoType)
    const element = videoWithContextRenderer.querySelector('.media-item[data-id="' + id + '"]')
    element.remove();
}