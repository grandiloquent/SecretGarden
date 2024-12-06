const bottomSheetContainer = document.querySelector('.bottom-sheet-container');

const bottomSheetScrim = document.querySelector('.bottom-sheet-scrim');
bottomSheetScrim.addEventListener('click', evt => {
    evt.preventDefault();
    evt.stopImmediatePropagation();
    bottomSheetContainer.style.display = 'none';
})
// const bottomSheetRenderer = document.querySelector('.bottom-sheet-renderer');
// bottomSheetRenderer.addEventListener('click', evt => {
//     evt.preventDefault();
//     bottomSheetContainer.style.display = 'block';

// })
function formatDuration(ms) {
    if (isNaN(ms)) return '0:00';
    if (ms < 0) ms = -ms;
    const time = {
        hour: Math.floor(ms / 3600) % 24,
        minute: Math.floor(ms / 60) % 60,
        second: Math.floor(ms) % 60,
    };
    return Object.entries(time)
        .filter((val, index) => index || val[1])
        .map(val => (val[1] + '').padStart(2, '0'))
        .join(':');
}
const en_US = [
    "刚刚", "秒前",
    "1 分钟前", "分钟前",
    "1 小时前", "小时前",
    "1 天前", "天前",
    "1 周前", "周前",
    "1 个月前", "个月前",
    "1 年前", "年前"
]
const SECONDS_IN_TIME = [
    1,         // 1 second
    60,        // 1 minute
    3600,      // 1 hour
    86400,     // 1 day
    604800,    // 1 week
    2419200,   // 1 month
    29030400   // 1 year
];
function timeago(timestamp) {
    let now = Math.floor(new Date / 1000);
    let diff = (now - timestamp) || 1; // prevent undefined val when diff == 0
    for (let i = 6; i >= 0; i--) {
        if (diff >= SECONDS_IN_TIME[i]) {
            let time_elapsed = Math.floor(diff / SECONDS_IN_TIME[i]);
            let adverbs = en_US;
            let sentence = adverbs.map((el, idx) => idx % 2 == 0 ? el : time_elapsed + " " + el);
            return time_elapsed >= 2 ? sentence[i * 2 + 1] : sentence[i * 2];
        }

    }

}
const videoWithContextRenderer = document.querySelector('.video-with-context-renderer');

function render() {
    videoWithContextRenderer.innerHTML = '';
    if (NativeAndroid != undefined) {
        const videos = JSON.parse(NativeAndroid.loadVideos(null, 0, 1));
        const buffer = [];
        videos.forEach(video => {
            buffer.push(`<div class="media-item" data-id="${video.id}">
                    <div class="media-item-thumbnail-container">
                        <div class="video-thumbnail-container-large">
                            <div class="video-thumbnail-bg">
                            </div>
                            <img class="video-thumbnail-img lazy" data-src="${video.thumbnail}">
                        </div>
                        <div class="time-display">
                            <div class="time-display-wrapper">
                                <div class="badge-shape">
                                ${formatDuration(video.duration)}
                                </div>
                            </div>
                        </div>
                    </div>
    
                    <div class="details">
                        <div class="media-channel" style="display:none">
                            <a>
                                <div class="profile-icon">
                                    <img class="profile-icon-image"
                                        src="https://yt3.ggpht.com/ytc/AIdro_mwTLOIJf5a-29E8ip454Dtebsq48ukFFQ9gh0J9iudI-w=s68-c-k-c0x00ffffff-no-rj">
                                </div>
                            </a>
                        </div>
                        <div class="media-item-info">
                            <div class="media-item-metadata">
                                <a>
                                    <h3 class="media-item-headline">${video.title}</h3>
                                    <div class="badge-and-byline-renderer">
                                        <span class="badge-and-byline-item-byline">
                                          ${video.views} 次
                                        </span>
                                        <span class="badge-and-byline-separator">•</span>
                                    <span class="badge-and-byline-item-byline">
                                    ${timeago(video.createAt)}
                                        </span>
                                        </div>
                                </a>
                            </div>
                            <div class="bottom-sheet-renderer">
                                <div class="button-shape">
                                    <button class="spec-button">
    
                                        <div class="c3-icon">
                                            <svg xmlns="http://www.w3.org/2000/svg" enable-background="new 0 0 24 24"
                                                height="24" viewBox="0 0 24 24" width="24" focusable="false"
                                                aria-hidden="true"
                                                style="pointer-events: none; display: inherit; width: 100%; height: 100%;">
                                                <path
                                                    d="M12 16.5c.83 0 1.5.67 1.5 1.5s-.67 1.5-1.5 1.5-1.5-.67-1.5-1.5.67-1.5 1.5-1.5zM10.5 12c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5-.67-1.5-1.5-1.5-1.5.67-1.5 1.5zm0-6c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5-.67-1.5-1.5-1.5-1.5.67-1.5 1.5z">
                                                </path>
                                            </svg>
                                        </div>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>`);
        })
        videoWithContextRenderer.insertAdjacentHTML('afterbegin', buffer.join(''))
    }
    var lazyLoadInstance = new LazyLoad({
        // Your custom settings go here
    });
}
render();
