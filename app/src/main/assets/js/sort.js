

const sortList = document.querySelector('.sort-list');
sortList.addEventListener('click', evt => {
    evt.preventDefault();
    evt.stopImmediatePropagation();
    bottomSheetContent.innerHTML = [
        "发布时间最晚",
        "发布时间最早",
        "更新时间最晚",
        "播放次数最多",
        "播放次数最少",
        "已播更新最晚",
        "看过次数"].map((x, k) => {
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
                switch (parseInt(element.dataset.id)) {
                    case 1:
                        mSort = 0;
                        break;
                    case 2:
                        mSort = 1;
                        break;
                    case 3:
                        mSort = 3;
                        break;
                    case 4:
                        mSort = 5;
                        break;
                    case 5:
                        mSort = 9;
                        break;
                    case 6:
                        mSort = 6;
                        break;
                    case 7:
                        mSort = 7;
                        break;
                }
                localStorage.setItem('sort', mSort)
                videoWithContextRenderer.innerHTML = '';
                mOffset = 0;
                render()
            })
        });
})