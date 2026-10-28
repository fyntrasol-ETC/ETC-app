import SwiftUI

// MARK: - Sign In As Teacher
struct TeacherSignInScreen: View {
    @EnvironmentObject var teacherVM: TeacherAuthViewModel
    @EnvironmentObject var authVM: AuthViewModel
    var onBack: () -> Void = {}
    var onSignedIn: () -> Void = {}
    @Environment(\.appSizeClass) var sc

    @State private var name = ""
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        ScrollView {
            VStack(spacing: pad.lg) {
                HStack {
                    ETCBackButton(title: "Sign in as Teacher", action: onBack)
                    Spacer()
                }
                .padding(.top, pad.sm)

                Text("Use the same email and password you use to log in to this app.")
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if let error = teacherVM.errorMessage {
                    ErrorBanner(message: error)
                }

                VStack(spacing: pad.md) {
                    ETCTextField(label: "Full name", placeholder: "Full Name", text: $name, leadingIcon: "person")
                    ETCTextField(label: "Email", placeholder: "you@example.com", text: $email, leadingIcon: "envelope")
                    ETCTextField(label: "Password", placeholder: "Your login password", text: $password, leadingIcon: "lock", isSecure: true)
                }

                ETCButton(title: "Sign in as Teacher", isLoading: teacherVM.isLoading) {
                    Task {
                        await teacherVM.signInAsTeacher(name: name, email: email, password: password)
                        if teacherVM.isTeacherSignedIn { onSignedIn() }
                    }
                }
            }
            .padding(.horizontal, pad.lg)
            .frame(maxWidth: AppSizing.maxContentWidth(sc))
            .frame(maxWidth: .infinity)
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .onAppear {
            email = authVM.currentUser?.email ?? ""
        }
    }
}

// MARK: - Teacher Dashboard
struct TeacherDashboardScreen: View {
    @EnvironmentObject var teacherVM: TeacherAuthViewModel
    @ObservedObject private var classService = LiveClassService.shared
    var onBack: () -> Void = {}
    @Environment(\.appSizeClass) var sc
    @State private var showAddClass = false

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 2) {
                Text("My Classes")
                    .font(AppTheme.Typography.title2(sc))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                Text("Everyone's scheduled sessions")
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, pad.lg)
            .padding(.top, pad.lg)
            .padding(.bottom, pad.sm)

            ScrollView {
                VStack(alignment: .leading, spacing: pad.md) {
                    if let teacher = teacherVM.currentTeacher {
                        Text("Welcome, \(teacher.name)")
                            .font(AppTheme.Typography.title2(sc))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        Text(teacher.email)
                            .font(AppTheme.Typography.subheadline(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    }

                    Button { showAddClass = true } label: {
                        HStack {
                            Image(systemName: "plus.circle.fill")
                            Text("Add a live class").fontWeight(.semibold)
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(AppTheme.Colors.primaryFallback)
                        .cornerRadius(AppSizing.cardRadius(sc))
                    }

                    let allClasses = classService.activeClasses.sorted { $0.date < $1.date }
                    if allClasses.isEmpty {
                        Text("No classes have been scheduled yet.")
                            .font(AppTheme.Typography.subheadline(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    } else {
                        ForEach(allClasses) { cls in
                            TeacherClassRow(
                                liveClass: cls,
                                isMine: cls.teacherId == teacherVM.currentTeacher?.uid,
                                onDelete: {
                                    Task { try? await classService.deleteClass(cls) }
                                }
                            )
                        }
                    }

                    Button {
                        teacherVM.signOutAsTeacher()
                        onBack()
                    } label: {
                        Text("Exit teacher mode")
                            .foregroundColor(.red)
                            .font(AppTheme.Typography.subheadline(sc))
                    }
                    .padding(.top, pad.sm)
                }
                .padding(.horizontal, pad.lg)
                .padding(.top, pad.md)
                .padding(.bottom, 100)
            }
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .onAppear { classService.listen() }
        .fullScreenCover(isPresented: $showAddClass) {
            AddLiveClassScreen(onBack: { showAddClass = false })
                .environmentObject(teacherVM)
                .appSizeClass()
        }
    }
}

private struct TeacherClassRow: View {
    let liveClass: LiveClass
    let isMine: Bool
    var onDelete: () -> Void = {}
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        HStack(alignment: .top, spacing: pad.sm) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(liveClass.title)
                        .font(AppTheme.Typography.headline(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    if isMine {
                        Text("Yours")
                            .font(.system(size: AppSizing.fontSize(base: 10, sizeClass: sc), weight: .bold))
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 2)
                            .background(AppTheme.Colors.primaryLightFallback)   // already dynamic
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                }
                Text(liveClass.date.formatted(date: .abbreviated, time: .shortened))
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                Text("with \(liveClass.teacherName)")
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            Spacer()
            if isMine {
                Button(action: onDelete) {
                    Image(systemName: "trash")
                        .foregroundColor(.red)
                        .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc)))
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(pad.md)
        .background(AppTheme.Colors.surface)     // was: Color.white
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius, x: 0, y: AppTheme.Shadow.card.y)
    }
}

// MARK: - Add Live Class
struct AddLiveClassScreen: View {
    @EnvironmentObject var teacherVM: TeacherAuthViewModel
    @ObservedObject private var classService = LiveClassService.shared
    var onBack: () -> Void = {}
    @Environment(\.appSizeClass) var sc

    @State private var title = ""
    @State private var meetingLink = ""
    @State private var date = Date()
    @State private var isSaving = false
    @State private var errorMessage: String?

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        ScrollView {
            VStack(spacing: pad.lg) {
                HStack {
                    ETCBackButton(title: "Add Class", action: onBack)
                    Spacer()
                }
                .padding(.top, pad.sm)

                if let errorMessage { ErrorBanner(message: errorMessage) }

                VStack(spacing: pad.md) {
                    ETCTextField(label: "Class title", placeholder: "e.g. Pronunciation Clinic", text: $title, leadingIcon: "text.book.closed")
                    ETCTextField(label: "Meeting link", placeholder: "Zoom / Google Meet URL", text: $meetingLink, leadingIcon: "link")

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Date & time")
                            .font(AppTheme.Typography.subheadline(sc))
                            .fontWeight(.semibold)
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        DatePicker("", selection: $date, in: Date()..., displayedComponents: [.date, .hourAndMinute])
                            .datePickerStyle(.graphical)
                            .labelsHidden()
                    }
                }

                ETCButton(title: "Save class", isLoading: isSaving) { save() }
            }
            .padding(.horizontal, pad.lg)
            .frame(maxWidth: AppSizing.maxContentWidth(sc))
            .frame(maxWidth: .infinity)
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
    }

    private func save() {
        errorMessage = nil
        guard let teacher = teacherVM.currentTeacher else {
            errorMessage = "You must be signed in as a teacher."; return
        }
        guard !title.trimmingCharacters(in: .whitespaces).isEmpty,
              !meetingLink.trimmingCharacters(in: .whitespaces).isEmpty,
              URL(string: meetingLink) != nil else {
            errorMessage = "Please enter a class title and a valid meeting link."; return
        }

        isSaving = true
        let newClass = LiveClass(teacherId: teacher.uid, teacherName: teacher.name,
                                  title: title, meetingLink: meetingLink, date: date)
        Task {
            do {
                try await classService.addClass(newClass)
                isSaving = false
                onBack()
            } catch {
                isSaving = false
                errorMessage = error.localizedDescription
            }
        }
    }
}
