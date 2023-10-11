#include <jni.h>
#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#include <android/log.h>
#include <dirent.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <ctype.h>
#include "native-lib.h"


// https://github.com/tencentyun/qcloud-iot-explorer-sdk-embedded-c/blob/master/include/exports/qcloud_iot_export_error.h
// https://github.com/tencentyun/qcloud-iot-explorer-sdk-embedded-c/blob/master/platform/os/linux/HAL_TCP_linux.c
// https://android.googlesource.com/platform/bionic/+/ics-mr0/libc/string
static int
hexchar(const char *s) {
    unsigned char result[2];
    int i;

    for (i = 0; i < 2; i++) {
        if (s[i] >= '0' && s[i] <= '9')
            result[i] = (unsigned char) (s[i] - '0');
        else if (s[i] >= 'a' && s[i] <= 'f')
            result[i] = (unsigned char) (s[i] - 'a') + 10;
        else if (s[i] >= 'A' && s[i] <= 'F')
            result[i] = (unsigned char) (s[i] - 'A') + 10;
        else
            return -1;
    }
    return (result[0] << 4) | result[1];
}

static char *
urldecode(const char *src) {
    char *ret, *dst;
    int ch;

    ret = malloc(strlen(src) + 1);
    for (dst = ret; *src != '\0'; src++) {
        switch (*src) {
            case '+':
                *dst++ = ' ';
                break;
            case '%':
                if (!isxdigit((unsigned char) src[1]) ||
                    !isxdigit((unsigned char) src[2]) ||
                    (ch = hexchar(src + 1)) == -1) {
                    free(ret);
                    return NULL;
                }
                *dst++ = ch;
                src += 2;
                break;
            default:
                *dst++ = *src;
                break;
        }
    }
    *dst = '\0';

    return ret;
}


#define TOTAL_VAL_COUNT 254
int byteval_array[TOTAL_VAL_COUNT] = {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
        11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
        31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
        51, 52, 53, 54, 55, 56, 57, 58, 59, 60,
        61, 62, 63, 64, 65, 66, 67, 68, 69, 70,
        71, 72, 73, 74, 75, 76, 77, 78, 79, 80,
        81, 82, 83, 84, 85, 86, 87, 88, 89, 90,
        91, 92, 93, 94, 95, 96, 97, 98, 99, 100,
        101, 102, 103, 104, 105, 106, 107, 108, 109, 110,
        111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
        121, 122, 123, 124, 125, 126, 127, 128, 129, 130,
        131, 132, 133, 134, 135, 136, 137, 138, 139, 140,
        141, 142, 143, 144, 145, 146, 147, 148, 149, 150,
        151, 152, 153, 154, 155, 156, 157, 158, 159, 160,
        161, 162, 163, 164, 165, 166, 167, 168, 169, 170,
        171, 172, 173, 174, 175, 176, 177, 178, 179, 180,
        181, 182, 183, 184, 185, 186, 187, 188, 189, 190,
        191, 192, 193, 194, 195, 196, 197, 198, 199, 200,
        201, 202, 203, 204, 205, 206, 207, 208, 209, 210,
        211, 212, 213, 214, 215, 216, 217, 218, 219, 220,
        221, 222, 223, 224, 225, 226, 227, 228, 229, 230,
        231, 232, 233, 234, 235, 236, 237, 238, 239, 240,
        241, 242, 243, 244, 245, 246, 247, 248, 249, 250,
        251, 252, 253, 254
};


unsigned char denominator = TOTAL_VAL_COUNT + 1;

unsigned char generate_byte_val();

unsigned char generate_byte_val() {
    unsigned char inx, random_val;

    if (denominator == 1)
        denominator = TOTAL_VAL_COUNT + 1;
    inx = rand() % denominator;
    random_val = byteval_array[inx];
    byteval_array[inx] = byteval_array[--denominator];
    byteval_array[denominator] = random_val;
    return random_val;
}

int substr(char *s, char *start, char *end, char *buf) {
    const char *tmp = strdup(s);
    char *found = strstr(tmp, start);
    if (found == NULL) {
        free(tmp);
        return 0;
    }
    found += strlen(start);

    char *e = strstr(found, end);
    if (e == NULL) {
        free(tmp);
        return 0;
    }
    for (int i = 0; i < e - found; ++i) {
        buf[i] = *(found + i);
    }
    buf[e - found] = 0;
    free(tmp);
    return e - found;
}

char *get_cookie() {
    uintptr_t client = HAL_TCP_Connect("91porn.com", 80);
    if (!client)return NULL;
    char *buf = malloc(1024);
    // view_video.php?viewkey=%s
    memset(buf, 0, 1024);
    snprintf(buf, 1024, "GET /index.php HTTP/1.1\r\n"
                        "Host: 91porn.com\r\n"
                        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36\r\n"
                        "\r\n");


    size_t written_len = 0;
    int ret = HAL_TCP_Write(client, buf, strlen(buf), 3000, &written_len);

    size_t read_len = 0;

    ret = HAL_TCP_Read(client, buf, 1024, 3000, &read_len);
//    FILE *f = fopen("/storage/emulated/0/Android/data/euphoria.psycho.porn/files/1.txt",
//                    "w");
//    fwrite(buf, 1, sizeof (buf), f);
//    fclose(f);

    char *save = malloc(256);
    memset(save, 0, 256);
    if (!substr(buf, "Set-Cookie: ", ";", save)) {
        free(buf);
        return NULL;
    }
    HAL_TCP_Disconnect(client);
    //LOGE("%s",buf);
    free(buf);
    return save;
}

JNIEXPORT jstring JNICALL
Java_psycho_euphoria_v_Native_fetch91Porn(JNIEnv *env, jclass clazz, jstring url) {

    /*
    char *k = get_cookie();
    if (k == NULL) {
        return NULL;
    }
    LOGE("Get the cooike returned by the server: %s\n", k);
    char *url_ = (char *) (*env)->GetStringUTFChars(env, url, NULL);
    uintptr_t client = HAL_TCP_Connect("91porn.com", 80);
    if (!client)return NULL;
    LOGE("Connect to server: %lu\n", client);

    char *buf = malloc(40960);
    if(buf==NULL)return NULL;
    memset(buf, 0, 40960);
    struct in_addr ip;
    ip.s_addr = (generate_byte_val() |
                 (generate_byte_val() << 8) |
                 (generate_byte_val() << 16) |
                 (generate_byte_val() << 24));
    snprintf(buf, 512, "GET /view_video.php?viewkey=%s HTTP/1.1\r\n"
                       "Host: 91porn.com\r\n"
                       "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8\r\n"
                       "Cookie: %s\r\n"
                       "X-Forwarded-For: %s\r\n"
                       "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36\r\n"
                       "\r\n",
             url_, k, inet_ntoa(ip));
    free(k);

    (*env)->ReleaseStringUTFChars(env, url, url_);


    size_t written_len = 0;
    int ret = HAL_TCP_Write(client, buf, strlen(buf), 3000, &written_len);

    size_t read_len = 0;
    memset(buf, 0, 40960);

    ret = HAL_TCP_Read(client, buf, 40960, 3000, &read_len);


    HAL_TCP_Disconnect(client);
    char *title = malloc(256);
    title[0] = 0;
    if (!substr(buf, "<title>", "</title>", title)) {
        free(buf);
        free(title);
        return NULL;
    }
//    FILE *f = fopen("/storage/emulated/0/Android/data/euphoria.psycho.porn/files/1.txt",
//                    "w");
//    fwrite(buf, 1, sizeof(buf), f);
//    fclose(f);

    char *rr = malloc(256);

    if (!substr(buf, "strencode(\"", "\")", rr)) {
        free(buf);
        free(title);
        free(rr);
        return NULL;
    }

//    const char *r = urldecode(rr);
//    char save[256];
//    save[0] = 0;
//    LOGE("%d %s\n", 10,r);
//    if (!substr(r, "src='", "'", save)) {
//        free(r);
//        return NULL;
//    }
//    LOGE("%d\n", 11);
//    free(r);
//    memset(rr, 0, 256);
//    LOGE("%d\n", 12);
//    sprintf(rr, "%s|%s", title, save);
    char *tmp = malloc(1024);
    memset(tmp, 0, 1024);
    sprintf(tmp, "%s|%s", title, rr);
    free(buf);
    free(title);
    free(rr);
    return (*env)->NewStringUTF(env, tmp);
     */

    char *url_ = (char *) (*env)->GetStringUTFChars(env, url, NULL);
    uintptr_t client = HAL_TCP_Connect("kingpunch.cn", 80);

    char buf[2048];
    memset(buf, 0, 2048);
    snprintf(buf, 512,
             "GET /api/videos/4?q=http://91porn.com/view_video.php?viewkey=%s HTTP/1.1\r\n"
             "Host: kingpunch.cn\r\n"
             "\r\n",
             url_);
    (*env)->ReleaseStringUTFChars(env, url, url_);


    size_t written_len = 0;
    int ret = HAL_TCP_Write(client, buf, strlen(buf), 3000, &written_len);

    size_t read_len = 0;

    ret = HAL_TCP_Read(client, buf, 2048, 3000, &read_len);
    HAL_TCP_Disconnect(client);
//    char *b = strstr(buf, "\r\n\r\n");
//    if (b == NULL) return NULL;
//    if (!strstr(b, "{") || strstr(b, "\"\""))return NULL;
//    b += 4;
//    b = strstr(b, "\r\n");
//    if (b == NULL) return NULL;
//    b += 2;
//    char *save = b;
//    int i = 0;
//
//
//    while (*(b + 1) && *b++ != '}') {
//        i++;
//    }
//    save[i + 1] = 0;

    return (*env)->NewStringUTF(env, strstr(buf, "{"));

}

JNIEXPORT jstring JNICALL
Java_psycho_euphoria_v_Native_fetchCk(JNIEnv *env, jclass clazz, jstring url, jstring cookie,
                                         jstring userAgent) {
    char *url_ = (char *) (*env)->GetStringUTFChars(env, url, NULL);
    char *cookie_ = cookie != NULL ? (char *) (*env)->GetStringUTFChars(env, cookie, NULL) : NULL;
    char *userAgent_ =
            userAgent != NULL ? (char *) (*env)->GetStringUTFChars(env, userAgent, NULL) : NULL;

    char host[128];
    char *path = url_;
    if (strncmp(url_, "http://", 7) != 0)
        return NULL;
    path += 7;
    char *save = path;
    strncpy(host, save, strlen(save));
    while (*path++ != '/');
    host[path - save] = 0;
    if (path[0] != '/')
        path--;
    if (host[strlen(host) - 1] == '/')
        host[strlen(host) - 1] = 0;

    uintptr_t client = HAL_TCP_Connect(host, 80);

    int size=20480 << 1;
    char buf[size];

    memset(buf, 0, size);
    snprintf(buf, 1024, "GET %s HTTP/1.1\r\n"
                        "Host: %s\r\n"
                        "Cookie: %s\r\n"
                        "User-Agent: %s\r\n"
                        "\r\n",
             path, host, cookie_, userAgent_);

    (*env)->ReleaseStringUTFChars(env, url, url_);
    if (cookie != NULL)
        (*env)->ReleaseStringUTFChars(env, cookie, cookie_);
    if (userAgent != NULL)
        (*env)->ReleaseStringUTFChars(env, userAgent, userAgent_);

    size_t written_len = 0;
    int ret = HAL_TCP_Write(client, buf, strlen(buf), 10000, &written_len);

    size_t read_len = 0;

    buf[0] = 0;
    ret = HAL_TCP_Read(client, buf, size- 1, 10000, &read_len);
    HAL_TCP_Disconnect(client);
//    FILE *f = fopen("/storage/emulated/0/Android/data/euphoria.psycho.porn/files/1.txt",
//                    "w");
//    fwrite(buf, 1, sizeof(buf), f);
//    fclose(f);

    //if (ret != 0)return NULL;
//    char *b = strstr(buf, "\r\n\r\n");
//
//    if (b == NULL) return NULL;
//    b += 4;
//    b = strstr(b, "\r\n");
//    if (b == NULL) return NULL;
//    b += 2;

    char title[128];
    memset(title, 0, 128);
    char m3[128];
    m3[0] = 0;
    substring(buf, "<title>", "详情介绍", title);
    LOGE("%s", title);
    if (strlen(title) == 0) {
        return NULL;
    }
//    FILE *fp = fopen("/storage/emulated/0/.gs_file/1.txt", "w");
//    fputs(buf, fp);
//    fclose(fp);
    substring(buf, "\"link_pre\":\"\",\"url\":\"", "\",\"url_next\"", m3);

    char r[256];
    r[0] = 0;
    snprintf(r, 256, "%s\n%s", title, m3);
    return (*env)->NewStringUTF(env, r);
}
/*
char *last_char_is(const char *s, int c) {
    if (s && *s) {
        size_t sz = strlen(s) - 1;
        s += sz;
        if ((unsigned char) *s == c)
            return (char *) s;
    }
    return NULL;
}

int
xasprintf(char **ret, const char *fmt, ...) {
    va_list ap;
    int i;
    va_start(ap, fmt);
    i = vasprintf(ret, fmt, ap);
    va_end(ap);
    if (i < 0 || *ret == NULL)
        return i;
    //fatal("xasprintf: could not allocate memory");
    return (i);
}

char *concat_path_file(const char *path, const char *filename) {
    char *lc;
    if (!path)
        path = "";
    lc = last_char_is(path, '/');
    while (*filename == '/')
        filename++;
    return xasprintf("%s%s%s", path, (lc == NULL ? "/" : ""), filename);
}

// https://coral.googlesource.com/busybox/+/refs/tags/1_30_0/util-linux/switch_root.c
// Recursively delete contents of rootfs
static void delete_contents(const char *directory) {
    DIR *dir;
    struct dirent *d;
    struct stat st;
    // Don't descend into other filesystems
    if (lstat(directory, &st))
        return;
    // Recursively delete the contents of directories
    if (S_ISDIR(st.st_mode)) {
        dir = opendir(directory);
        if (dir) {
            while ((d = readdir(dir))) {
                char *newdir = d->d_name;
                // Skip . and ..
                if (DOT_OR_DOTDOT(newdir))
                    continue;
                // Recurse to delete contents
                newdir = concat_path_file(directory, newdir);
                delete_contents(newdir);
                free(newdir);
            }
            closedir(dir);
            // Directory should now be empty, zap it
            rmdir(directory);
        }
    } else {
        // It wasn't a directory, zap it
        unlink(directory);
    }
}
*/

JNIEXPORT void JNICALL
Java_psycho_euphoria_v_Native_removeDirectory(JNIEnv *env, jclass clazz, jstring directory) {
    char *directory_ =
            directory != NULL ? (char *) (*env)->GetStringUTFChars(env, directory, NULL) : NULL;
    if (directory == NULL)
        return;

    unlink_recursive(directory_);

    (*env)->ReleaseStringUTFChars(env, directory, directory_);


}
// https://service-nljbtijs-1301282710.hk.apigw.tencentcs.com/release/
JNIEXPORT jstring JNICALL
Java_euphoria_psycho_porn_Native_getUrl(JNIEnv *env, jclass clazz) {
    const char *uri = "http://47.106.105.122/videos";
    return (*env)->NewStringUTF(env, uri);
}

JNIEXPORT jstring JNICALL
Java_euphoria_psycho_porn_Native_getUri(JNIEnv *env, jclass clazz) {
    const char *uri = "http://47.106.105.122/x";
    return (*env)->NewStringUTF(env, uri);
}