

const videoList = document.querySelector('.video-list');
videoList.addEventListener('click', evt => {
    evt.preventDefault();
    evt.stopImmediatePropagation();
    bottomSheetContent.innerHTML = [
        "91",
        "57",
        "收藏",
        "屏蔽",
        "露脸",
        "其他",
        "视频"].map((x, k) => {
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
                mVideoType = parseInt(element.dataset.id)
                localStorage.setItem('videoType', element.dataset.id)
                videoWithContextRenderer.innerHTML = '';
                mOffset = 0;
                render()
            })
        });
})