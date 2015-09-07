#include <jni.h>  
#include <stdio.h>     
#include <stdlib.h>     
#include <fcntl.h>  
#include <unistd.h>     
#include <sys/ioctl.h>     
#include <android/log.h>  
  
#define LOG_TAG "LED"       //android logcat  
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__    )  
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS_    _)  
  
void signal_handler(int signum) {
	LOGI("RECEIVE SIGNAL %d \n", signum);
	if (signum == SIGUSR2) {

	}

}

JNIEXPORT jint JNICALL Java_com_medicapture_usb400camera_MainActivity_led
  (JNIEnv *env, jclass thiz, jint led_nu, jint on)
{    
	pid_t pid;
    int fd;
	const int DEFAULT_BRIGHTNESS = 255;
      
	//pid = getpid();
	//LOGI("PID = %d\n", pid );

	static int RunOnce = 0;
	if ( RunOnce == 0 ){
		/*
		struct sigaction act;
		act.sa_handler = signal_handler;
		sigemptyset(&act.sa_mask);
		act.sa_flags = 0;

		if (sigaction(SIGUSR2, &act, NULL) < 0)
			LOGI("sigaction error");
		*/
		signal(SIGUSR2, signal_handler);	// Using sigaction is better
		RunOnce++;
	}

	switch ( led_nu )
	{
	case 0:
		fd = open("/sys/class/leds/led:rgb_blue/brightness", O_RDWR);  
		break;
	case 1:
		fd = open("/sys/class/leds/led:rgb_green/brightness", O_RDWR);
		break;
	case 2:
		fd = open("/sys/class/leds/led:rgb_red/brightness", O_RDWR);
		break;
	}
    if(fd < 0)    
		LOGI("Can't open /dev/leds!\n");
	
	if ( on )
		write( fd, "255", 3 );
	else
		write( fd, "0", 1 );

    //LOGI("led_nu=%d,state=%d\n", led_nu, on);
    close(fd);    
      
    return 0;    
}  
