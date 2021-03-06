/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <unistd.h>
#include <sysexits.h>

#include <base/logging.h>
#include <base/command_line.h>
#include <base/macros.h>
#include <base/bind.h>
#include <binderwrapper/binder_wrapper.h>
#include <brillo/binder_watcher.h>
#include <brillo/daemons/daemon.h>
#include <brillo/syslog_logging.h>

#include <mraa.h>

class MyDaemon final : public brillo::Daemon {
public:
	MyDaemon() = default;
protected:
	int OnInit() override;
	void toggleLED(mraa_gpio_context gpio);
private:
	/* the bridge between libbinder and brillo::MessageLoop */
	brillo::BinderWatcher binder_watcher_;

	base::WeakPtrFactory<MyDaemon> weak_ptr_factory_{this};
	DISALLOW_COPY_AND_ASSIGN(MyDaemon);
};

#define IO_LED		25

int MyDaemon::OnInit()
{
	int rc;
	if ((rc = brillo::Daemon::OnInit()) != EX_OK)
		return rc;

	/* Create and initialize the singleton for communicating with the real binder system */
	android::BinderWrapper::Create();
	if (!binder_watcher_.Init())
		return EX_OSERR;

	mraa_init();
	LOG(INFO) << "hello mraa running on " << mraa_get_platform_name();
	mraa_gpio_context gpio = mraa_gpio_init(IO_LED);
	mraa_gpio_dir(gpio, MRAA_GPIO_OUT);

	toggleLED(gpio);

	return EX_OK;
}

void MyDaemon::toggleLED(mraa_gpio_context gpio)
{
	mraa_gpio_write(gpio, !mraa_gpio_read(gpio));

	brillo::MessageLoop::current()->PostDelayedTask(
		base::Bind(&MyDaemon::toggleLED, weak_ptr_factory_.GetWeakPtr(), gpio),
		base::TimeDelta::FromMilliseconds(500));
}

int main(int argc, char* argv[])
{
	base::CommandLine::Init(argc, argv);
	brillo::InitLog(brillo::kLogToSyslog | brillo::kLogHeader);
	MyDaemon daemon;
	return daemon.Run();
}
