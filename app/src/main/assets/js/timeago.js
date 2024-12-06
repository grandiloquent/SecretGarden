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