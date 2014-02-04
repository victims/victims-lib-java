Name:           victims-lib-java
Version:        1.3.1
Release:        1%{?dist}
Summary:        Fingerprinting and service interaction for the Victims Project

License:        AGPLv3+
URL:            https://github.com/victims/victims-lib-java
Source0:        https://github.com/victims/victims-lib-java/archive/victims-lib-%{version}.tar.gz
BuildArch:	noarch

BuildRequires:	maven-local
BuildRequires:	google-gson
BuildRequires:	h2

%description
A java library providing fingerprinting and service interaction for
the Victims Project.


%package javadoc
Summary:	Javadocs for %{name}
Group:		Documentation

%description javadoc
This package contains the API documentation for %{name}.

%prep
%autosetup -p1 -n %{name}-victims-lib-%{version}

%build
%mvn_build

%install
%mvn_install

%files -f .mfiles
%dir %{_javadir}/%{name}

%files javadoc -f .mfiles-javadoc

%changelog
* Tue Feb  4 2014 Florian Weimer <fweimer@redhat.com> - 1.3.1-1
- Initial packaging
